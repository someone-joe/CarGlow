package com.ruoyi.wash.order.service;

import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.goods.domain.WashService;
import com.ruoyi.wash.goods.service.WashServiceQueryService;
import com.ruoyi.wash.member.domain.WashVehicle;
import com.ruoyi.wash.member.service.WashVehicleQueryService;
import com.ruoyi.wash.network.domain.WashCabinet;
import com.ruoyi.wash.network.service.WashCabinetQueryService;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.dto.CreateOrderRequest;
import com.ruoyi.wash.order.dto.CreateOrderVO;
import com.ruoyi.wash.order.mapper.WashOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 下单。契约：openapi.yaml POST /api/v1/orders。
 *
 * <p>校验顺序（契约描述）：协议已同意 → 服务项有效 → 车辆属于本人 → 机柜有效 → 预约时间合法 → 产能未超。
 *
 * <p>已实现：幂等、参数与归属校验、产能原子抢占、价格快照、支付倒计时。
 * 未实现（对应模块未开工，不得假装做了）：格口预占（PRD 要求下单即预占格口，格口表与柜机模块未建）。
 */
@Service
public class WashOrderCreateService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final String IDEM_PREFIX = "order:idem:";
    private static final String CAPACITY_PREFIX = "capacity:";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private WashOrderMapper orderMapper;

    @Autowired
    private WashServiceQueryService serviceQueryService;

    @Autowired
    private WashVehicleQueryService vehicleQueryService;

    @Autowired
    private WashCabinetQueryService cabinetQueryService;

    @Autowired
    private RedisCache redisCache;

    /** 产能用原子 DECR，必须用 StringRedisTemplate 而不是先读再写 */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 支付倒计时（分钟），阈值走配置，不硬编码 */
    @Value("${wash.order.pay-timeout-minutes:15}")
    private int payTimeoutMinutes;

    @Transactional(rollbackFor = Exception.class)
    public CreateOrderVO create(Long memberId, CreateOrderRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(ErrorCode.A0001, "缺少幂等键 Idempotency-Key");
        }

        // 幂等：同一键重复提交直接返回首次结果，不重复建单
        String idemKey = IDEM_PREFIX + idempotencyKey;
        String existedOrderNo = redisCache.getCacheObject(idemKey);
        if (existedOrderNo != null) {
            WashOrder existed = orderMapper.selectByOrderNo(existedOrderNo);
            if (existed != null) {
                return buildVO(existed, existed.getPayAmount(), existed.getPayExpireAt());
            }
        }

        if (!Boolean.TRUE.equals(request.agreed())) {
            throw new ApiException(ErrorCode.A0001, "请先同意《服务条款与钥匙寄存协议》");
        }

        WashService service = serviceQueryService.requireEnabled(request.serviceId());
        WashVehicle vehicle = vehicleQueryService.requireOwned(memberId, request.vehicleId());
        WashCabinet cabinet = cabinetQueryService.requireEnabled(request.cabinetId());

        long appointTime = parseAppointTime(request.appointDate(), request.appointTime());
        if (appointTime <= System.currentTimeMillis()) {
            throw new ApiException(ErrorCode.A0001, "预约时间必须晚于当前时间");
        }

        // 产能：capacity:{siteId}:{date}，未初始化（null）表示当日不限量，不做拦截
        takeCapacity(cabinet.getSiteId(), request.appointDate());

        long now = System.currentTimeMillis();
        String orderNo = generateOrderNo(now);
        long payExpireAt = now + payTimeoutMinutes * 60L * 1000;

        WashOrder order = new WashOrder();
        order.setOrderNo(orderNo);
        order.setMemberId(memberId);
        order.setSiteId(cabinet.getSiteId());
        order.setCommunityId(cabinet.getCommunityId());
        order.setServiceId(request.serviceId());
        order.setVehicleId(vehicle.getVehicleId());
        order.setCabinetId(cabinet.getCabinetId());
        order.setStatus(OrderStatus.WAIT_PAY.name());
        order.setServiceName(service.getServiceName());
        order.setAppointTime(appointTime);
        order.setPayAmount(service.getPriceAmount());
        order.setPayExpireAt(payExpireAt);
        order.setPlateNo(vehicle.getPlateNo());
        order.setRemark(request.remark());
        order.setDelFlag("0");
        orderMapper.insertWashOrder(order);

        // 幂等记录保留 24 小时，覆盖重复提交与网络重试窗口
        redisCache.setCacheObject(idemKey, orderNo, 24, TimeUnit.HOURS);

        return buildVO(order, service.getPriceAmount(), payExpireAt);
    }

    /**
     * 原子抢占名额。
     *
     * <p>踩过的坑：直接 DECR 一个不存在的 key，Redis 会把它建成 -1 并返回 -1，
     * 「未设置产能」会被误判成「已满」。因此用 Lua 区分三种结果：
     * 未设置（-1，不限制） / 已满（0，拒绝） / 抢到（1）。
     */
    private static final RedisScript<Long> TAKE_CAPACITY_SCRIPT = RedisScript.of(
            "local v = redis.call('GET', KEYS[1]) " +
                    "if not v then return -1 end " +
                    "if tonumber(v) <= 0 then return 0 end " +
                    "redis.call('DECR', KEYS[1]) " +
                    "return 1",
            Long.class);

    private void takeCapacity(Long siteId, String appointDate) {
        String key = CAPACITY_PREFIX + siteId + ":" + appointDate;
        Long result = stringRedisTemplate.execute(TAKE_CAPACITY_SCRIPT, List.of(key));
        if (result != null && result == 0L) {
            throw new ApiException(ErrorCode.A0006);
        }
    }

    private CreateOrderVO buildVO(WashOrder order, Long payAmount, Long payExpireAt) {
        CreateOrderVO vo = new CreateOrderVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setPayAmount(payAmount);
        vo.setPayExpireAt(payExpireAt);
        return vo;
    }

    /** 订单号：SE + 年月日时分秒 + 6 位随机，示例 SE202609151601276839838 */
    private String generateOrderNo(long now) {
        String time = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT).format(new java.util.Date(now));
        int suffix = 100000 + RANDOM.nextInt(900000);
        return "SE" + time + suffix;
    }

    /** "2026-09-18" + "19:00" → 毫秒时间戳（Asia/Shanghai） */
    private long parseAppointTime(String date, String time) {
        try {
            String[] hhmm = time.split(":");
            LocalDateTime dateTime = LocalDate.parse(date)
                    .atTime(Integer.parseInt(hhmm[0]), hhmm.length > 1 ? Integer.parseInt(hhmm[1]) : 0);
            return dateTime.atZone(ZONE).toInstant().toEpochMilli();
        } catch (Exception e) {
            throw new ApiException(ErrorCode.A0001, "预约时间格式错误，期望 date=yyyy-MM-dd、time=HH:mm");
        }
    }
}
