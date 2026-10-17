package com.ruoyi.wash.order.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.mapper.WashOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 师傅端订单查询与派单落库（任务池数据源）。
 *
 * <p>只负责查数据 + 派单结果的条件更新，VO 组装与业务校验在 wash-worker：
 * 订单域不感知师傅端的展示语义（PICKUP / RETURN 分组、照片要求、派单规则等）。
 */
@Service
public class WashOrderPickService {

    @Autowired
    private WashOrderMapper orderMapper;

    /** 站点任务池（旧口径，全量可见）；派单上线后师傅端改用 selectByWorkerAndStatuses */
    public List<WashOrder> selectBySiteAndStatuses(Long siteId, List<String> statuses) {
        return orderMapper.selectBySiteAndStatuses(siteId, statuses);
    }

    /** 派给某师傅的订单 */
    public List<WashOrder> selectByWorkerAndStatuses(Long workerId, List<String> statuses) {
        return orderMapper.selectByWorkerAndStatuses(workerId, statuses);
    }

    /** 待派单池：本站点、尚未派单的订单 */
    public List<WashOrder> selectUnassignedBySiteAndStatuses(Long siteId, List<String> statuses) {
        return orderMapper.selectUnassignedBySiteAndStatuses(siteId, statuses);
    }

    /** 派单结果落库。返回 0 表示已被他人先派走（并发保护，调用方据此判定抢单失败） */
    public int assignWorker(String orderNo, Long workerId, long assignedAt) {
        return orderMapper.assignWorker(orderNo, workerId, assignedAt);
    }

    /** 后台改派：无条件覆盖（含已派给别人的单），返回 0 表示订单不存在 */
    public int forceAssignWorker(String orderNo, Long workerId, long assignedAt) {
        return orderMapper.forceAssignWorker(orderNo, workerId, assignedAt);
    }

    /** 按订单号取订单（派单校验归属/小区/状态用） */
    public WashOrder requireByOrderNo(String orderNo) {
        WashOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new ApiException(ErrorCode.B1001);
        }
        return order;
    }
}
