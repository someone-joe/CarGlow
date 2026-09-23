package com.ruoyi.wash.order.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.dto.OrderActionVO;
import com.ruoyi.wash.order.dto.OrderPageVO;
import com.ruoyi.wash.order.mapper.WashOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单查询。Tab 与状态的映射在此唯一定义，前端不得自行推断状态集合。
 */
@Service
public class WashOrderQueryService {

    /** 「待处理」= 客户还差一步动作：去支付 / 去存钥匙 */
    private static final List<OrderStatus> PENDING_STATUSES = List.of(
            OrderStatus.WAIT_PAY, OrderStatus.WAIT_KEY);

    /** 「服务中」= 钥匙已入柜起：取车 → 运输 → 清洗 → 质检 → 还车 → 待客户取回钥匙 */
    private static final List<OrderStatus> SERVING_STATUSES = List.of(
            OrderStatus.KEY_IN, OrderStatus.PICKING, OrderStatus.TO_STATION, OrderStatus.WASHING,
            OrderStatus.QC, OrderStatus.WAIT_RETURN, OrderStatus.RETURNING, OrderStatus.RETURNED);

    /** 「已完成」= 收尾与终态：待评价、已完成、已取消、退款中、已退款 */
    private static final List<OrderStatus> DONE_STATUSES = List.of(
            OrderStatus.WAIT_REVIEW, OrderStatus.FINISHED, OrderStatus.CANCELED,
            OrderStatus.REFUNDING, OrderStatus.REFUNDED);

    /** 旧分组「进行中」的完整状态集，仅用于兼容老版本小程序传来的 ONGOING */
    private static final List<OrderStatus> LEGACY_ONGOING_STATUSES = List.of(
            OrderStatus.WAIT_PAY, OrderStatus.WAIT_KEY, OrderStatus.KEY_IN, OrderStatus.PICKING,
            OrderStatus.TO_STATION, OrderStatus.WASHING, OrderStatus.QC,
            OrderStatus.WAIT_RETURN, OrderStatus.RETURNING, OrderStatus.RETURNED);

    /** 作业中：车已被取走，进入运输/清洗/质检/还车环节（客户此时只能看进度、找客服） */
    private static final List<OrderStatus> WORKING_STATUSES = List.of(
            OrderStatus.TO_STATION, OrderStatus.WASHING, OrderStatus.QC,
            OrderStatus.WAIT_RETURN, OrderStatus.RETURNING);

    /** 单页上限，防止一次性拉全量 */
    private static final int MAX_PAGE_SIZE = 50;

    @Autowired
    private WashOrderMapper orderMapper;

    public OrderPageVO page(Long memberId, String tab, int pageNum, int pageSize) {
        List<String> statuses = switch (tab == null ? "PENDING" : tab) {
            case "PENDING" -> nameOf(PENDING_STATUSES);
            case "SERVING" -> nameOf(SERVING_STATUSES);
            case "DONE" -> nameOf(DONE_STATUSES);
            // 旧取值兼容：改版前的小程序包仍在传，按语义归到新分组（不抛未知 tab）
            case "ONGOING" -> nameOf(LEGACY_ONGOING_STATUSES);
            case "WAIT_REVIEW" -> List.of(OrderStatus.WAIT_REVIEW.name());
            case "ALL" -> null;
            default -> throw new ApiException(ErrorCode.A0001, "未知 tab：" + tab);
        };

        int size = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int num = Math.max(pageNum, 1);

        List<OrderPageVO.OrderListItemVO> list = orderMapper
                .selectPageByMember(memberId, statuses, (num - 1) * size, size)
                .stream()
                .map(this::toVO)
                .toList();
        long total = orderMapper.countByMember(memberId, statuses);

        OrderPageVO vo = new OrderPageVO();
        vo.setList(list);
        vo.setTotal(total);
        vo.setPageNum(num);
        vo.setPageSize(size);
        return vo;
    }

    private static List<String> nameOf(List<OrderStatus> statuses) {
        return statuses.stream().map(Enum::name).toList();
    }

    private OrderPageVO.OrderListItemVO toVO(WashOrder order) {
        OrderPageVO.OrderListItemVO vo = new OrderPageVO.OrderListItemVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        // 展示名直接取状态机枚举，保证与规则表一字不差
        vo.setStatusLabel(OrderStatus.of(order.getStatus()).getLabel());
        vo.setServiceName(order.getServiceName());
        vo.setAppointTime(order.getAppointTime());
        vo.setPlateNo(order.getPlateNo());
        vo.setPayAmount(order.getPayAmount());
        vo.setSubActions(List.of());
        // 按钮由后端返回（契约 OrderAction），前端不得自行推断
        vo.setMainAction(resolveMainAction(order.getStatus()));
        vo.setSubActions(resolveSubActions(order.getStatus()));
        return vo;
    }

    /**
     * 主按钮规则：取值来自契约 OrderAction 的枚举，前端按 action 分发，不得自行推断。
     *
     * <p>已结束（已完成 / 已取消 / 退款中 / 已退款）一律回落为「再来一单」，
     * 避免客户在终态订单上看到无按钮的死界面。
     */
    public OrderActionVO resolveMainAction(String status) {
        OrderStatus s = OrderStatus.of(status);
        if (s == OrderStatus.WAIT_PAY) {
            return new OrderActionVO("PAY", "去支付", true);
        }
        if (s == OrderStatus.WAIT_KEY) {
            return new OrderActionVO("DEPOSIT_KEY", "去存钥匙", true);
        }
        if (s == OrderStatus.RETURNED) {
            return new OrderActionVO("TAKE_KEY", "取钥匙", true);
        }
        if (s == OrderStatus.WAIT_REVIEW) {
            return new OrderActionVO("REVIEW", "去评价", true);
        }
        if (s == OrderStatus.FINISHED || s == OrderStatus.CANCELED
                || s == OrderStatus.REFUNDING || s == OrderStatus.REFUNDED) {
            return new OrderActionVO("REORDER", "再来一单", true);
        }
        // 车已取走或在作业中：客户最想知道的是进度
        return new OrderActionVO("VIEW_PROGRESS", "看进度", true);
    }

    /**
     * 次按钮。
     *
     * <p>能否取消一律以状态机的 isCustomerCancelable() 为准，不在此另写一份状态名单（避免规则两处漂移）。
     * 车已取走或订单已结束时，给客户留一条找人的通道（联系客服）。
     */
    public List<OrderActionVO> resolveSubActions(String status) {
        OrderStatus s = OrderStatus.of(status);
        List<OrderActionVO> subs = new ArrayList<>();
        if (s.isCustomerCancelable()) {
            subs.add(new OrderActionVO("CANCEL", "取消订单", true));
        }
        if (s == OrderStatus.WAIT_REVIEW) {
            subs.add(new OrderActionVO("REORDER", "再来一单", true));
        }
        if (WORKING_STATUSES.contains(s) || s == OrderStatus.RETURNED
                || s == OrderStatus.FINISHED || s == OrderStatus.REFUNDING) {
            subs.add(new OrderActionVO("CONTACT_SERVICE", "联系客服", true));
        }
        return subs;
    }
}
