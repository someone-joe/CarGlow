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
import java.util.List;
import java.util.Locale;

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

    // ---------------- 内部 ----------------

    private List<String> statusesForTab(String tab) {
        String t = tab == null ? "WAIT_USE" : tab;
        return switch (t) {
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
