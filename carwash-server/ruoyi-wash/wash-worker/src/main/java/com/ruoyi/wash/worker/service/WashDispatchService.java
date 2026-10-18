package com.ruoyi.wash.worker.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.event.OrderKeyInEvent;
import com.ruoyi.wash.order.service.WashOrderPickService;
import com.ruoyi.wash.worker.domain.WashWorker;
import com.ruoyi.wash.worker.domain.WorkerWorkStatus;
import com.ruoyi.wash.worker.mapper.WashWorkerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动派单 / 抢单 / 改派（T13）。
 *
 * <p>准入条件（三条全中才是候选）：账号正常、已打卡上班、资质全过（人脸核身 + 后台审核）。
 * 择优顺序：同小区优先 → 进行中订单最少 → 工号小者（保证结果稳定可复现）。
 * 站点是硬边界：师傅只看本站点单，派单也不会跨站。
 *
 * <p>派单无候选时不报错——订单进「待派单池」，由在线师傅抢单或后台手动派单。
 */
@Service
public class WashDispatchService {

    private static final Logger log = LoggerFactory.getLogger(WashDispatchService.class);

    /** 进行中的订单状态（负载均衡计数用） */
    private static final List<String> ACTIVE_STATUSES = List.of(
            OrderStatus.KEY_IN.name(), OrderStatus.PICKING.name(), OrderStatus.TO_STATION.name(),
            OrderStatus.WASHING.name(), OrderStatus.QC.name(), OrderStatus.WAIT_RETURN.name(),
            OrderStatus.RETURNING.name(), OrderStatus.RETURNED.name());

    @Autowired
    private WashOrderPickService pickQuery;
    @Autowired
    private WashWorkerMapper workerMapper;
    @Autowired
    private WashWorkerService workerService;
    @Autowired
    private WashWorkerQualificationService qualificationService;
    @Autowired
    private WashWorkerScheduleService scheduleService;

    /**
     * 钥匙入柜 → 自动派单。由 {@link OrderKeyInEvent} 监听入口调用，失败只记日志：
     * 派不派得出去不该影响「钥匙已入柜」这个既成事实。
     */
    @Transactional(rollbackFor = Exception.class)
    public void dispatchOnKeyIn(String orderNo, Long communityId, Long siteId) {
        WashOrder order = pickQuery.requireByOrderNo(orderNo);
        if (order.getWorkerId() != null) {
            return;
        }
        List<WashWorker> candidates = eligibleWorkers(siteId);
        WashWorker best = pickBest(candidates, communityId);
        if (best == null) {
            log.info("[派单] 无合格候选，订单进入待派单池 orderNo={} siteId={}", orderNo, siteId);
            return;
        }
        int n = pickQuery.assignWorker(orderNo, best.getWorkerId(), best.getName(), System.currentTimeMillis());
        if (n == 0) {
            log.info("[派单] 并发下已被他人派走 orderNo={}", orderNo);
            return;
        }
        log.info("[派单] 自动派单成功 orderNo={} workerId={}({}) sameCommunity={}",
                orderNo, best.getWorkerId(), best.getName(), communityId != null && communityId.equals(best.getCommunityId()));
    }

    /** 抢单：待派单池里的单，在线且资质合格的师傅可接 */
    @Transactional(rollbackFor = Exception.class)
    public void claim(Long workerId, String orderNo) {
        WashWorker worker = requireClaimable(workerId);
        WashOrder order = pickQuery.requireByOrderNo(orderNo);
        if (order.getWorkerId() != null) {
            throw new ApiException(ErrorCode.B1002, "手慢了，该单已被他人接走");
        }
        if (!worker.getSiteId().equals(order.getSiteId())) {
            throw new ApiException(ErrorCode.A0003, "只能接本站点的单");
        }
        int n = pickQuery.assignWorker(orderNo, workerId, worker.getName(), System.currentTimeMillis());
        if (n == 0) {
            throw new ApiException(ErrorCode.B1002, "手慢了，该单已被他人接走");
        }
        log.info("[派单] 抢单成功 orderNo={} workerId={}", orderNo, workerId);
    }

    /** 后台手动派单 / 改派：无条件覆盖，用于纠正自动派单结果 */
    @Transactional(rollbackFor = Exception.class)
    public void dispatch(String orderNo, Long workerId) {
        WashOrder order = pickQuery.requireByOrderNo(orderNo);
        WashWorker worker = workerMapper.selectByWorkerId(workerId);
        if (worker == null || !"NORMAL".equals(worker.getStatus())) {
            throw new ApiException(ErrorCode.A0001, "师傅不存在或已禁用");
        }
        if (!worker.getSiteId().equals(order.getSiteId())) {
            throw new ApiException(ErrorCode.A0001, "不能跨站点派单");
        }
        if (pickQuery.forceAssignWorker(orderNo, workerId, worker.getName(), System.currentTimeMillis()) == 0) {
            throw new ApiException(ErrorCode.A0001, "订单不存在");
        }
        log.info("[派单] 后台派单/改派 orderNo={} workerId={}", orderNo, workerId);
    }

    /** 后台派单下拉：本站点合格师傅（按同小区 + 负载排序） */
    public List<WashWorker> candidates(Long siteId, Long communityId) {
        List<WashWorker> all = eligibleWorkers(siteId);
        List<WashWorker> same = new ArrayList<>();
        List<WashWorker> other = new ArrayList<>();
        for (WashWorker w : all) {
            if (communityId != null && communityId.equals(w.getCommunityId())) {
                same.add(w);
            } else {
                other.add(w);
            }
        }
        same.addAll(other);
        return same;
    }

    /** 我的任务：派给该师傅的订单 */
    public List<WashOrder> myTasks(Long workerId, List<String> statuses) {
        return pickQuery.selectByWorkerAndStatuses(workerId, statuses);
    }

    /** 后台改派用：该师傅进行中的全部订单 */
    public List<WashOrder> activeTasks(Long workerId) {
        return pickQuery.selectByWorkerAndStatuses(workerId, ACTIVE_STATUSES);
    }

    /** 待派单池：本站点未派单的订单 */
    public List<WashOrder> poolTasks(Long siteId, List<String> statuses) {
        return pickQuery.selectUnassignedBySiteAndStatuses(siteId, statuses);
    }

    /** 后台手动派单：本站点待派单池（未派单 + 进行中状态） */
    public List<WashOrder> dispatchPool(Long siteId) {
        return pickQuery.selectUnassignedBySiteAndStatuses(siteId, ACTIVE_STATUSES);
    }

    /** 派单绩效：单量维度（好评/投诉待评价域接入后补） */
    public long activeCount(Long workerId) {
        return pickQuery.selectByWorkerAndStatuses(workerId, ACTIVE_STATUSES).size();
    }

    // ---------------- 内部 ----------------

    private List<WashWorker> eligibleWorkers(Long siteId) {
        List<WashWorker> out = new ArrayList<>();
        for (WashWorker w : workerMapper.selectDispatchCandidates(siteId)) {
            if (!qualificationService.isPassed(w.getWorkerId())) {
                continue;
            }
            if (!scheduleService.isOnDutyToday(w.getWorkerId())) {
                continue;
            }
            out.add(w);
        }
        return out;
    }

    /** 择优：同小区优先 → 负载最小 → 工号小者 */
    private WashWorker pickBest(List<WashWorker> candidates, Long communityId) {
        WashWorker best = null;
        boolean bestSame = false;
        long bestLoad = 0;
        for (WashWorker w : candidates) {
            boolean same = communityId != null && communityId.equals(w.getCommunityId());
            long load = activeCount(w.getWorkerId());
            if (best == null || (same && !bestSame) || (same == bestSame && load < bestLoad)) {
                best = w;
                bestSame = same;
                bestLoad = load;
            }
        }
        return best;
    }

    private WashWorker requireClaimable(Long workerId) {
        WashWorker worker = workerService.require(workerId);
        if (!"NORMAL".equals(worker.getStatus())) {
            throw new ApiException(ErrorCode.A0003, "账号不可用");
        }
        WorkerWorkStatus work = WorkerWorkStatus.of(worker.getWorkStatus());
        if (work != WorkerWorkStatus.ONLINE && work != WorkerWorkStatus.BUSY) {
            throw new ApiException(ErrorCode.A0003, "请先打卡上班再抢单");
        }
        if (!qualificationService.isPassed(workerId)) {
            throw new ApiException(ErrorCode.A0003, "资质未通过，暂不能接单");
        }
        return worker;
    }
}
