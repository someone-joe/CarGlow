package com.ruoyi.wash.order.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.dto.OrderPageVO;
import com.ruoyi.wash.order.mapper.WashOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单查询。Tab 与状态的映射在此唯一定义，前端不得自行推断状态集合。
 */
@Service
public class WashOrderQueryService {

    /** 「进行中」= 从待支付到已还车（不含待评价及之后） */
    private static final List<OrderStatus> ONGOING_STATUSES = List.of(
            OrderStatus.WAIT_PAY, OrderStatus.WAIT_KEY, OrderStatus.KEY_IN, OrderStatus.PICKING,
            OrderStatus.TO_STATION, OrderStatus.WASHING, OrderStatus.QC,
            OrderStatus.WAIT_RETURN, OrderStatus.RETURNING, OrderStatus.RETURNED);

    /** 单页上限，防止一次性拉全量 */
    private static final int MAX_PAGE_SIZE = 50;

    @Autowired
    private WashOrderMapper orderMapper;

    public OrderPageVO page(Long memberId, String tab, int pageNum, int pageSize) {
        List<String> statuses = switch (tab == null ? "ONGOING" : tab) {
            case "ONGOING" -> ONGOING_STATUSES.stream().map(Enum::name).toList();
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
        return vo;
    }
}
