package com.ruoyi.wash.selfwash.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.goods.domain.WashService;
import com.ruoyi.wash.goods.service.WashServiceQueryService;
import com.ruoyi.wash.pay.service.WashWalletService;
import com.ruoyi.wash.selfwash.domain.SelfWashStatus;
import com.ruoyi.wash.selfwash.domain.WashSelfwashOrder;
import com.ruoyi.wash.selfwash.domain.WashWorkstation;
import com.ruoyi.wash.selfwash.dto.SelfwashOrderVO;
import com.ruoyi.wash.selfwash.device.SelfWashDeviceService;
import com.ruoyi.wash.selfwash.mapper.WashSelfwashOrderMapper;
import com.ruoyi.wash.selfwash.mapper.WashWorkstationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 自助洗车订单：选位下单 → 支付（储值/微信mock）→ 待使用 → 取消（已付退余额）。
 *
 * <p>Stage 2b 追加：到店激活/排队/软件倒计时/加时/到点结束 + 定时任务 + 设备通电压·断电 mock。
 */
@Service
public class WashSelfwashOrderService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long PAY_EXPIRE_MS = 30 * 60 * 1000L;

    @Autowired
    private WashSelfwashOrderMapper orderMapper;
    @Autowired
    private WashWorkstationMapper workstationMapper;
    @Autowired
    private WashServiceQueryService serviceQuery;
    @Autowired
    private WashWalletService walletService;

    @Value("${wash.pay.mock-enabled:false}")
    private boolean mockEnabled;

    @Autowired
    private SelfWashDeviceService deviceService;

    /** 选位下单：快照服务价/时长，建 WAIT_PAY 订单。 */
    @Transactional(rollbackFor = Exception.class)
    public String create(Long memberId, Long workstationId, Long serviceId) {
        if (workstationId == null || serviceId == null) {
            throw new ApiException(ErrorCode.A0001, "参数不完整");
        }
        WashWorkstation ws = workstationMapper.selectById(workstationId);
        if (ws == null || !"Y".equals(ws.getEnabled())) {
            throw new ApiException(ErrorCode.A0001, "工位不存在或已停用");
        }
        WashService service = serviceQuery.requireEnabled(serviceId);
        if (service.getPriceAmount() == null || service.getPriceAmount() <= 0) {
            throw new ApiException(ErrorCode.A0001, "服务价格异常");
        }
        WashSelfwashOrder order = new WashSelfwashOrder();
        order.setOrderNo(generateNo("SW"));
        order.setMemberId(memberId);
        order.setSiteId(ws.getSiteId());
        order.setWorkstationId(workstationId);
        order.setServiceId(serviceId);
        order.setServiceName(service.getServiceName());
        order.setServiceMinutes(service.getWorkMinutes() == null ? 0 : service.getWorkMinutes());
        order.setPayAmount(service.getPriceAmount());
        order.setStatus(SelfWashStatus.WAIT_PAY.name());
        order.setAddedSeconds(0);
        order.setPayExpireAt(System.currentTimeMillis() + PAY_EXPIRE_MS);
        orderMapper.insert(order);
        return order.getOrderNo();
    }

    /** 储值余额支付：扣余额 → WAIT_USE。与扣款同一事务。 */
    @Transactional(rollbackFor = Exception.class)
    public void payWithBalance(Long memberId, String orderNo) {
        WashSelfwashOrder order = requireOwned(memberId, orderNo, SelfWashStatus.WAIT_PAY);
        checkPayNotExpired(order);
        walletService.deductForOrder(memberId, orderNo, order.getPayAmount());
        if (orderMapper.markPaid(orderNo, "BALANCE") == 0) {
            throw new ApiException(ErrorCode.B1002, "订单状态已变化，请刷新");
        }
    }

    /** 微信支付（本地 mock，生产由支付回调替代）。受 wash.pay.mock-enabled 控制。 */
    @Transactional(rollbackFor = Exception.class)
    public void payMock(Long memberId, String orderNo) {
        if (!mockEnabled) {
            throw new ApiException(ErrorCode.A0003, "当前环境未开启模拟支付");
        }
        WashSelfwashOrder order = requireOwned(memberId, orderNo, SelfWashStatus.WAIT_PAY);
        checkPayNotExpired(order);
        if (orderMapper.markPaid(orderNo, "WECHAT_MOCK") == 0) {
            throw new ApiException(ErrorCode.B1002, "订单状态已变化，请刷新");
        }
    }

    /** 列表。tab：WAIT_USE 待使用 / ONGOING 进行中(排队+服务+暂停) / DONE 已完成 / CANCELED 已取消。 */
    public List<SelfwashOrderVO> list(Long memberId, String tab) {
        List<String> statuses = statusesForTab(tab);
        List<SelfwashOrderVO> out = new ArrayList<>();
        for (WashSelfwashOrder o : orderMapper.selectByMemberTab(memberId, statuses)) {
            out.add(toVO(o));
        }
        return out;
    }

    public SelfwashOrderVO detail(Long memberId, String orderNo) {
        return toVO(requireOwnedAny(memberId, orderNo));
    }

    /** 取消：WAIT_PAY 直接取消；WAIT_USE 且储值支付的退回余额。服务中不可取消。 */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long memberId, String orderNo, String reason) {
        WashSelfwashOrder order = requireOwnedAny(memberId, orderNo);
        if (order.getStatus() == null
                || (!SelfWashStatus.WAIT_PAY.name().equals(order.getStatus())
                && !SelfWashStatus.WAIT_USE.name().equals(order.getStatus()))) {
            throw new ApiException(ErrorCode.B1002, "当前状态不可取消（服务中/已结束不可取消）");
        }
        int n = orderMapper.markCanceled(orderNo, reason == null ? "" : reason);
        if (n == 0) {
            throw new ApiException(ErrorCode.B1002, "订单状态已变化，请刷新");
        }
        // 条件取消成功，且原为「储值支付的待使用」才退余额，避免重复退
        if (SelfWashStatus.WAIT_USE.name().equals(order.getStatus()) && "BALANCE".equals(order.getPayChannel())) {
            walletService.refundToWallet(memberId, orderNo, order.getPayAmount());
        }
    }

    // ---------------- 到店激活 / 计时 / 结束（Stage 2b） ----------------

    /** 到店激活/开始服务：工位空闲则直接进入服务中（通电 + 倒计时）；有正在服务的则排队。 */
    @Transactional(rollbackFor = Exception.class)
    public SelfwashOrderVO activate(Long memberId, String orderNo) {
        WashSelfwashOrder order = requireOwned(memberId, orderNo, SelfWashStatus.WAIT_USE);
        List<WashSelfwashOrder> active = orderMapper.selectActiveByWorkstation(order.getWorkstationId());
        boolean servicing = active.stream().anyMatch(o -> SelfWashStatus.SERVING.name().equals(o.getStatus()));
        long now = System.currentTimeMillis();
        if (servicing) {
            int maxQueue = 0;
            for (WashSelfwashOrder o : active) {
                if (SelfWashStatus.QUEUING.name().equals(o.getStatus()) && o.getQueueNo() != null && o.getQueueNo() > maxQueue) {
                    maxQueue = o.getQueueNo();
                }
            }
            orderMapper.markQueued(orderNo, maxQueue + 1);
            order.setStatus(SelfWashStatus.QUEUING.name());
            order.setQueueNo(maxQueue + 1);
        } else {
            long expire = now + totalMinutes(order) * 60000L;
            orderMapper.markStarted(orderNo, now, expire);
            deviceService.powerOn(order.getWorkstationId(), orderNo);
            order.setStatus(SelfWashStatus.SERVING.name());
            order.setStartedAt(now);
            order.setExpireAt(expire);
        }
        return toVO(order);
    }

    /** 加时：延长服务截止（V1 免费加时，不计费）。 */
    @Transactional(rollbackFor = Exception.class)
    public SelfwashOrderVO addTime(Long memberId, String orderNo, int minutes) {
        if (minutes <= 0 || minutes > 240) {
            throw new ApiException(ErrorCode.A0001, "加时分钟数不合法（1-240）");
        }
        WashSelfwashOrder order = requireOwned(memberId, orderNo, SelfWashStatus.SERVING);
        long base = order.getExpireAt() == null ? System.currentTimeMillis() : order.getExpireAt();
        long newExpire = base + minutes * 60000L;
        orderMapper.addSeconds(orderNo, minutes * 60, newExpire);
        order.setExpireAt(newExpire);
        order.setAddedSeconds((order.getAddedSeconds() == null ? 0 : order.getAddedSeconds()) + minutes * 60);
        return toVO(order);
    }

    /** 手动结束服务：断电 → 完成 → 若该工位有排队则自动开始下一单。 */
    @Transactional(rollbackFor = Exception.class)
    public void finish(Long memberId, String orderNo) {
        WashSelfwashOrder order = requireOwned(memberId, orderNo, SelfWashStatus.SERVING);
        orderMapper.markDone(orderNo);
        deviceService.powerOff(order.getWorkstationId(), orderNo);
        startNextIfIdle(order.getWorkstationId());
    }

    /** 定时任务：到点仍在服务的订单，自动断电完成并开始下一单（逐单自动提交，避免长事务）。 */
    public void processExpiredServing() {
        List<WashSelfwashOrder> expired = orderMapper.selectExpiredServing(System.currentTimeMillis(), 50);
        for (WashSelfwashOrder o : expired) {
            if (orderMapper.markDone(o.getOrderNo()) > 0) {
                deviceService.powerOff(o.getWorkstationId(), o.getOrderNo());
                startNextIfIdle(o.getWorkstationId());
            }
        }
    }

    /** 工位无正在服务订单时，开始排队最前的一单（调用方需已在事务内或可自动提交）。 */
    private void startNextIfIdle(Long workstationId) {
        List<WashSelfwashOrder> active = orderMapper.selectActiveByWorkstation(workstationId);
        boolean servicing = false;
        WashSelfwashOrder next = null;
        for (WashSelfwashOrder o : active) {
            if (SelfWashStatus.SERVING.name().equals(o.getStatus())) {
                servicing = true;
            } else if (SelfWashStatus.QUEUING.name().equals(o.getStatus())) {
                if (next == null || (o.getQueueNo() != null && next.getQueueNo() != null && o.getQueueNo() < next.getQueueNo())) {
                    next = o;
                }
            }
        }
        if (servicing || next == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long expire = now + totalMinutes(next) * 60000L;
        orderMapper.markStarted(next.getOrderNo(), now, expire);
        deviceService.powerOn(workstationId, next.getOrderNo());
    }

    private long totalMinutes(WashSelfwashOrder o) {
        int base = o.getServiceMinutes() == null ? 0 : o.getServiceMinutes();
        int added = o.getAddedSeconds() == null ? 0 : o.getAddedSeconds();
        return base + added / 60L;
    }

    // ---------------- 后台监控 ----------------

    /** 后台分页（筛选：状态/站点/工位/会员/单号） */
    public Map<String, Object> adminPage(Long memberId, Long siteId, Long workstationId, String status,
                                         String orderNo, int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("siteId", siteId);
        params.put("workstationId", workstationId);
        params.put("status", status);
        params.put("orderNo", orderNo);
        params.put("offset", (pageNum - 1) * pageSize);
        params.put("limit", pageSize);
        Map<String, Object> data = new HashMap<>();
        List<SelfwashOrderVO> voRows = new ArrayList<>();
        for (WashSelfwashOrder o : orderMapper.selectAdminPage(params)) {
            voRows.add(toVO(o));
        }
        data.put("rows", voRows);
        data.put("total", orderMapper.countAdmin(params));
        return data;
    }

    /** 后台详情（不按会员隔离） */
    public SelfwashOrderVO adminDetail(String orderNo) {
        WashSelfwashOrder o = orderMapper.selectByOrderNo(orderNo);
        if (o == null) {
            throw new ApiException(ErrorCode.B1001, "订单不存在");
        }
        return toVO(o);
    }

    /** 后台强制结束（处理卡在服务中的异常单）：断电 + 完成 + 开始下一排队单 */
    @Transactional(rollbackFor = Exception.class)
    public void adminForceFinish(String orderNo) {
        WashSelfwashOrder o = orderMapper.selectByOrderNo(orderNo);
        if (o == null) {
            throw new ApiException(ErrorCode.B1001, "订单不存在");
        }
        if (!SelfWashStatus.SERVING.name().equals(o.getStatus())) {
            throw new ApiException(ErrorCode.B1002, "仅服务中的订单可强制结束，当前：" + label(o.getStatus()));
        }
        orderMapper.markDone(orderNo);
        deviceService.powerOff(o.getWorkstationId(), orderNo);
        startNextIfIdle(o.getWorkstationId());
    }

    /** 状态下拉（后台筛选用），值来自 SelfWashStatus */
    public List<Map<String, String>> statusOptions() {
        List<Map<String, String>> out = new ArrayList<>();
        for (SelfWashStatus s : SelfWashStatus.values()) {
            out.add(Map.of("value", s.name(), "label", s.getLabel()));
        }
        return out;
    }

    // ---------------- 内部 ----------------

    private List<String> statusesForTab(String tab) {
        String t = tab == null ? "WAIT_USE" : tab;
        return switch (t) {
            case "WAIT_PAY" -> List.of(SelfWashStatus.WAIT_PAY.name());
            case "ONGOING" -> Arrays.asList(SelfWashStatus.QUEUING.name(), SelfWashStatus.SERVING.name(), SelfWashStatus.PAUSED.name());
            case "DONE" -> List.of(SelfWashStatus.DONE.name());
            case "CANCELED" -> List.of(SelfWashStatus.CANCELED.name());
            default -> List.of(SelfWashStatus.WAIT_USE.name());
        };
    }

    private void checkPayNotExpired(WashSelfwashOrder order) {
        if (order.getPayExpireAt() != null && order.getPayExpireAt() < System.currentTimeMillis()) {
            throw new ApiException(ErrorCode.B3001);
        }
    }

    private WashSelfwashOrder requireOwned(Long memberId, String orderNo, SelfWashStatus expect) {
        WashSelfwashOrder order = requireOwnedAny(memberId, orderNo);
        if (!expect.name().equals(order.getStatus())) {
            throw new ApiException(ErrorCode.B1002, "订单状态不允许该操作，当前：" + label(order.getStatus()));
        }
        return order;
    }

    private WashSelfwashOrder requireOwnedAny(Long memberId, String orderNo) {
        WashSelfwashOrder order = orderMapper.selectByMemberAndNo(memberId, orderNo);
        if (order == null) {
            throw new ApiException(ErrorCode.B1001, "订单不存在");
        }
        return order;
    }

    private SelfwashOrderVO toVO(WashSelfwashOrder o) {
        SelfwashOrderVO vo = new SelfwashOrderVO();
        vo.setOrderNo(o.getOrderNo());
        vo.setSiteId(o.getSiteId());
        vo.setWorkstationId(o.getWorkstationId());
        vo.setServiceName(o.getServiceName());
        vo.setServiceMinutes(o.getServiceMinutes());
        vo.setPayAmount(o.getPayAmount());
        vo.setPayChannel(o.getPayChannel());
        vo.setStatus(o.getStatus());
        vo.setStatusLabel(label(o.getStatus()));
        vo.setQueueNo(o.getQueueNo());
        vo.setStartedAt(o.getStartedAt());
        vo.setExpireAt(o.getExpireAt());
        vo.setAddedSeconds(o.getAddedSeconds());
        vo.setPayExpireAt(o.getPayExpireAt());
        vo.setCancelReason(o.getCancelReason());
        vo.setCreateTime(o.getCreateTime());
        long remain = 0L;
        if (SelfWashStatus.SERVING.name().equals(o.getStatus()) && o.getExpireAt() != null) {
            remain = Math.max(0L, (o.getExpireAt() - System.currentTimeMillis()) / 1000L);
        }
        vo.setRemainingSeconds(remain);
        return vo;
    }

    private String label(String status) {
        try {
            return SelfWashStatus.of(status).getLabel();
        } catch (IllegalArgumentException e) {
            return status;
        }
    }

    private String generateNo(String prefix) {
        String time = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT).format(new Date());
        return prefix + time + (100000 + RANDOM.nextInt(900000));
    }
}
