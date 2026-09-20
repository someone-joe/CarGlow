package com.ruoyi.wash.worker.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.statemachine.OrderEvent;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.service.WashOrderStationService;
import com.ruoyi.wash.order.state.WashOrderStateService;
import com.ruoyi.wash.worker.dto.StationQueueVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 师傅端作业业务：工位队列看板、入场/驶离打卡、SOP 完成、质检通过/不合格。
 *
 * <p>状态一律走 WashOrderStateService.workerFire（统一状态机），禁止直接 update 订单状态。
 */
@Service
public class WashStationService {

    private static final Logger log = LoggerFactory.getLogger(WashStationService.class);

    @Autowired
    private WashWorkerService workerService;

    @Autowired
    private WashOrderStationService stationQuery;

    @Autowired
    private WashOrderStateService stateService;

    /** 累计重洗达到该次数就要人工复盘（PRD：≥2 次自动生成复盘工单），走配置不写死 */
    @Value("${wash.qc.rewash-alert:2}")
    private int rewashAlertThreshold;

    /** 工位队列看板：清洗中的进 bays，待质检的进 waiting。 */
    public StationQueueVO queue(Long workerId) {
        Long siteId = workerService.requireSiteId(workerId);
        List<WashOrder> orders = stationQuery.selectQueue(siteId);

        List<StationQueueVO.Bay> bays = new ArrayList<>();
        List<StationQueueVO.Waiting> waiting = new ArrayList<>();
        int washing = 0;
        int qc = 0;

        for (WashOrder order : orders) {
            if (OrderStatus.WASHING.name().equals(order.getStatus())) {
                washing++;
                StationQueueVO.Bay bay = new StationQueueVO.Bay();
                bay.setOrderNo(order.getOrderNo());
                bay.setStatus(order.getStatus());
                // bayNo / startedAt / sopStep：工位表与 SOP 明细尚未接入，先留空
                bays.add(bay);
            } else {
                qc++;
                StationQueueVO.Waiting item = new StationQueueVO.Waiting();
                item.setOrderNo(order.getOrderNo());
                item.setServiceName(order.getServiceName());
                waiting.add(item);
            }
        }

        StationQueueVO vo = new StationQueueVO();
        vo.setStationId(siteId);
        vo.setStationName(stationQuery.siteName(siteId));
        vo.setWashingCount(washing);
        vo.setQcCount(qc);
        vo.setBays(bays);
        vo.setWaiting(waiting);
        return vo;
    }

    /** 入场打卡：到中央站开始作业。 */
    public void arrive(String orderNo, Long workerId) {
        stateService.workerFire(orderNo, OrderEvent.ARRIVE_STATION, workerId, null);
    }

    /** 驶离站点：洗完开回小区。 */
    public void leave(String orderNo, Long workerId) {
        stateService.workerFire(orderNo, OrderEvent.LEAVE_STATION, workerId, null);
    }

    /** SOP 全部完成：15 步打卡明细后端按 step 记，这里汇总提交。 */
    public void sopDone(String orderNo, List<String> steps, Long workerId) {
        stateService.workerFire(orderNo, OrderEvent.SOP_DONE, workerId, steps == null ? null : String.join(",", steps));
    }

    /** 质检通过：必须带洗后照，否则无法界定洗后车况。 */
    public void qcPass(String orderNo, List<String> fileIds, Long workerId) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new ApiException(ErrorCode.B4002);
        }
        stateService.workerFire(orderNo, OrderEvent.QC_PASS, workerId, null);
    }

    /**
     * 质检不合格：打回返工，原因必填。
     * 重洗次数从流转日志统计（不另加订单列）；达到阈值说明反复返工，告警待人工复盘。
     */
    public void qcFail(String orderNo, String reason, Long workerId) {
        if (reason == null || reason.isBlank()) {
            throw new ApiException(ErrorCode.A0001, "质检不合格必须填写原因");
        }
        long before = stationQuery.countRewashByOrderNo(orderNo);
        stateService.workerFire(orderNo, OrderEvent.QC_FAIL, workerId, reason);
        long after = before + 1;
        if (after >= rewashAlertThreshold) {
            log.warn("[质检不合格] 告警：orderNo={} 累计重洗 {} 次，需人工复盘（复盘工单待售后域接入）", orderNo, after);
        }
    }
}
