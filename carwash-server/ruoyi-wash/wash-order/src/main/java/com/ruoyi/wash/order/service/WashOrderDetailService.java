package com.ruoyi.wash.order.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.common.statemachine.OrderStatusLog;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.dto.MediaVO;
import com.ruoyi.wash.order.dto.OrderActionVO;
import com.ruoyi.wash.order.dto.OrderDetailVO;
import com.ruoyi.wash.order.dto.TimelineNodeVO;
import com.ruoyi.wash.order.mapper.WashOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单详情与 C 端时间轴。
 *
 * <p>时间轴的唯一数据来源是 wash_order_status_log（规则表第七条：直接读它，不另写一套进度），
 * 节点与状态的映射见《订单状态机规则表.md》第三节。
 */
@Service
public class WashOrderDetailService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /** 8 个节点按顺序排列；值为触发该节点的状态（首次出现即视为到达） */
    private static final Map<String, String[]> NODE_TRIGGERS = new LinkedHashMap<>();

    static {
        NODE_TRIGGERS.put("ORDERED", new String[]{OrderStatus.WAIT_PAY.name()});
        NODE_TRIGGERS.put("KEY_IN", new String[]{OrderStatus.KEY_IN.name()});
        NODE_TRIGGERS.put("PICKED", new String[]{OrderStatus.PICKING.name(), OrderStatus.TO_STATION.name()});
        NODE_TRIGGERS.put("ARRIVED", new String[]{OrderStatus.WASHING.name()});
        NODE_TRIGGERS.put("WASHING", new String[]{OrderStatus.WASHING.name()});
        NODE_TRIGGERS.put("QC_DONE", new String[]{OrderStatus.WAIT_RETURN.name()});
        NODE_TRIGGERS.put("RETURNED", new String[]{OrderStatus.RETURNED.name()});
        NODE_TRIGGERS.put("WAIT_TAKE_KEY", new String[]{OrderStatus.WAIT_REVIEW.name()});
    }

    private static final Map<String, String> NODE_TITLES = Map.of(
            "ORDERED", "已下单",
            "KEY_IN", "钥匙已入柜",
            "PICKED", "已取车",
            "ARRIVED", "到达中央站",
            "WASHING", "清洗中",
            "QC_DONE", "质检完成",
            "RETURNED", "已还车",
            "WAIT_TAKE_KEY", "待取钥匙");

    @Autowired
    private WashOrderMapper orderMapper;

    @Autowired
    private WashOrderQueryService queryService;

    public OrderDetailVO detail(String orderNo, Long memberId) {
        WashOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || !order.getMemberId().equals(memberId)) {
            throw new ApiException(ErrorCode.B1001);
        }

        OrderStatus currentStatus = OrderStatus.of(order.getStatus());
        List<OrderStatusLog> logs = orderMapper.selectLogsByOrderId(order.getOrderId());

        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusLabel(currentStatus.getLabel());
        // 描述文案后续走字典/配置，这里先用状态名拼，避免硬编码业务承诺
        vo.setStatusDesc(currentStatus.getLabel());
        vo.setPlateNo(order.getPlateNo());
        vo.setServiceName(order.getServiceName());
        vo.setRemark(order.getRemark());
        vo.setOriginAmount(order.getPayAmount());
        vo.setDiscountAmount(0L);
        vo.setPayAmount(order.getPayAmount());
        vo.setOverdue(false);
        vo.setTimeline(buildTimeline(logs, currentStatus, order));
        vo.setMedias(List.of());
        vo.setMainAction(queryService.resolveMainAction(order.getStatus()));
        vo.setSubActions(List.of());
        return vo;
    }

    /**
     * 用流转日志生成时间轴：日志里出现过触发状态即视为已到达，
     * 否则为未到达（time 为空，前端灰显），当前状态对应的节点标 current。
     */
    private List<TimelineNodeVO> buildTimeline(List<OrderStatusLog> logs, OrderStatus currentStatus, WashOrder order) {
        Map<String, Long> reachedAt = new LinkedHashMap<>();
        for (OrderStatusLog log : logs) {
            for (Map.Entry<String, String[]> entry : NODE_TRIGGERS.entrySet()) {
                if (reachedAt.containsKey(entry.getKey())) {
                    continue;
                }
                for (String trigger : entry.getValue()) {
                    if (trigger.equals(log.getToStatus())) {
                        reachedAt.put(entry.getKey(), toMillis(log.getCreateTime()));
                        break;
                    }
                }
            }
        }
        // 「已下单」以订单创建时间为准（订单创建本身不产生流转日志）
        if (!reachedAt.containsKey("ORDERED") && order.getCreateTime() != null) {
            // BaseEntity 的 createTime 是 java.util.Date
            reachedAt.put("ORDERED", order.getCreateTime().getTime());
        }

        String currentNode = statusToNode(currentStatus);

        List<TimelineNodeVO> nodes = new ArrayList<>();
        for (String node : NODE_TRIGGERS.keySet()) {
            TimelineNodeVO item = new TimelineNodeVO();
            item.setNode(node);
            item.setTitle(NODE_TITLES.get(node));
            item.setTime(reachedAt.get(node));
            item.setReached(reachedAt.containsKey(node));
            item.setCurrent(node.equals(currentNode));
            item.setEvidences(List.of());
            nodes.add(item);
        }
        return nodes;
    }

    /** 当前状态落在时间轴的哪个节点上（用于前端高亮） */
    private String statusToNode(OrderStatus status) {
        return switch (status) {
            case WAIT_PAY, WAIT_KEY -> "ORDERED";
            case KEY_IN -> "KEY_IN";
            case PICKING, TO_STATION -> "PICKED";
            case WASHING -> "WASHING";
            case QC -> "ARRIVED";
            case WAIT_RETURN, RETURNING -> "QC_DONE";
            case RETURNED -> "RETURNED";
            case WAIT_REVIEW -> "WAIT_TAKE_KEY";
            case FINISHED -> "WAIT_TAKE_KEY";
            default -> "ORDERED";
        };
    }

    private long toMillis(java.time.LocalDateTime dateTime) {
        return dateTime.atZone(ZONE).toInstant().toEpochMilli();
    }
}
