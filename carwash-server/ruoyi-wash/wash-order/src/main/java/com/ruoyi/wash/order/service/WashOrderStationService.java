package com.ruoyi.wash.order.service;

import com.ruoyi.wash.common.statemachine.OrderEvent;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.network.service.WashSiteQueryService;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.mapper.WashOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 师傅端作业台数据源：中央站工位队列、重洗次数。
 *
 * <p>只提供数据，看板 VO 组装与业务校验在 wash-worker；
 * 订单域不感知作业台的展示语义。
 */
@Service
public class WashOrderStationService {

    /**
     * 作业台关注的四个状态：待入场（去程）/ 清洗中 / 待质检 / 待还车。
     * 必须覆盖入场与驶离，否则师傅在看板里看不到单，arrive / leave 就没有入口。
     */
    private static final List<String> QUEUE_STATUSES =
            List.of(OrderStatus.TO_STATION.name(), OrderStatus.WASHING.name(),
                    OrderStatus.QC.name(), OrderStatus.WAIT_RETURN.name());

    @Autowired
    private WashOrderMapper orderMapper;

    @Autowired
    private WashSiteQueryService siteQuery;

    /** 复用任务池那条按站点+状态查询，只是状态集合不同，不另写一条 SQL。 */
    public List<WashOrder> selectQueue(Long siteId) {
        return orderMapper.selectBySiteAndStatuses(siteId, QUEUE_STATUSES);
    }

    public String siteName(Long siteId) {
        return siteQuery.siteName(siteId);
    }

    /** 重洗次数（QC_FAIL 累计次数）。推进前调用得到的是"已重洗次数"。 */
    public long countRewashByOrderNo(String orderNo) {
        WashOrder order = orderMapper.selectByOrderNo(orderNo);
        return order == null ? 0L : orderMapper.countEventByOrderId(order.getOrderId(), OrderEvent.QC_FAIL.name());
    }
}
