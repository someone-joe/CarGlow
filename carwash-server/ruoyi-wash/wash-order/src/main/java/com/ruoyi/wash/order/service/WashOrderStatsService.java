package com.ruoyi.wash.order.service;

import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.order.dto.DashboardStatsVO;
import com.ruoyi.wash.order.dto.StatusCountVO;
import com.ruoyi.wash.order.mapper.WashOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台看板统计。
 *
 * <p>口径集中在这里定义（唯一处），前端只展示不计算：
 * <ul>
 *   <li>作业中 = 运输中（去程）+ 清洗中 + 待质检：车已被取走开始作业；</li>
 *   <li>异常 = 退款中：钱没退干净，必须人工跟进；</li>
 *   <li>待存钥匙 = WAIT_KEY：卡在客户侧，运营要催。</li>
 * </ul>
 * 状态取值一律引用 OrderStatus 枚举，禁止写状态字符串字面量（CODEBUDDY 第 4 节）。
 */
@Service
public class WashOrderStatsService {

    private static final List<String> WASHING_STATUSES = List.of(
            OrderStatus.TO_STATION.name(), OrderStatus.WASHING.name(), OrderStatus.QC.name());

    private static final List<String> ABNORMAL_STATUSES = List.of(OrderStatus.REFUNDING.name());

    @Autowired
    private WashOrderMapper orderMapper;

    public DashboardStatsVO stats() {
        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setTodayOrderCount(orderMapper.countSince(LocalDate.now().atStartOfDay()));
        vo.setWashingCount(orderMapper.countByStatuses(WASHING_STATUSES));
        vo.setWaitingKeyCount(orderMapper.countByStatuses(List.of(OrderStatus.WAIT_KEY.name())));
        vo.setAbnormalCount(orderMapper.countByStatuses(ABNORMAL_STATUSES));
        vo.setStatusBreakdown(breakdown());
        return vo;
    }

    /**
     * 状态分布：按 OrderStatus 枚举顺序输出（而非数据库 group by 的随机顺序），
     * 保证看板图表顺序与主链路一致；没有订单的状态也要补 0，否则图表会缺项。
     */
    private List<StatusCountVO> breakdown() {
        Map<String, Long> counts = new HashMap<>();
        for (Map<String, Object> row : orderMapper.countGroupByStatus()) {
            Object status = row.get("status");
            Object cnt = row.get("cnt");
            if (status != null && cnt != null) {
                counts.put(String.valueOf(status), ((Number) cnt).longValue());
            }
        }
        List<StatusCountVO> list = new ArrayList<>();
        for (OrderStatus status : OrderStatus.values()) {
            StatusCountVO item = new StatusCountVO();
            item.setStatus(status.name());
            item.setLabel(status.getLabel());
            item.setCount(counts.getOrDefault(status.name(), 0L));
            list.add(item);
        }
        return list;
    }
}
