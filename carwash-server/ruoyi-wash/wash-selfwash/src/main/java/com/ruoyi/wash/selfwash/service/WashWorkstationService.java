package com.ruoyi.wash.selfwash.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.selfwash.domain.SelfWashStatus;
import com.ruoyi.wash.selfwash.domain.WashSelfwashOrder;
import com.ruoyi.wash.selfwash.domain.WashWorkstation;
import com.ruoyi.wash.selfwash.dto.WorkstationQueueItemVO;
import com.ruoyi.wash.selfwash.dto.WorkstationVO;
import com.ruoyi.wash.selfwash.mapper.WashSelfwashOrderMapper;
import com.ruoyi.wash.selfwash.mapper.WashWorkstationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 自助洗车工位。
 *
 * <p>实时状态由自助订单推导：该工位有 SERVING 单则「服务中」（带剩余秒），否则「空闲」；queueCount 为排队人数。
 */
@Service
public class WashWorkstationService {

    @Autowired
    private WashWorkstationMapper mapper;
    @Autowired
    private WashSelfwashOrderMapper orderMapper;

    public List<WashWorkstation> adminList(Long siteId, String keyword) {
        return mapper.selectAdminList(siteId, keyword);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(Long siteId, String code, String name, String enabled, Integer sortOrder) {
        if (siteId == null) {
            throw new ApiException(ErrorCode.A0001, "请选择所属站点");
        }
        if (code == null || code.isBlank()) {
            throw new ApiException(ErrorCode.A0001, "工位编号必填，如 A1");
        }
        WashWorkstation ws = new WashWorkstation();
        ws.setSiteId(siteId);
        ws.setCode(code.trim());
        ws.setName(name);
        ws.setEnabled(enabled == null || enabled.isBlank() ? "Y" : enabled);
        ws.setSortOrder(sortOrder == null ? 0 : sortOrder);
        mapper.insert(ws);
        return ws.getWorkstationId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, String code, String name, String enabled, Integer sortOrder) {
        requireExists(id);
        WashWorkstation ws = new WashWorkstation();
        ws.setWorkstationId(id);
        ws.setCode(code);
        ws.setName(name);
        ws.setEnabled(enabled);
        ws.setSortOrder(sortOrder);
        mapper.update(ws);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireExists(id);
        mapper.softDelete(id);
    }

    /** C 端工位列表（带实时状态：服务中剩余秒 / 排队人数 / 预计等待 / 排队明细；车牌脱敏）。 */
    public List<WorkstationVO> listWithStatus(Long siteId) {
        List<WorkstationVO> out = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (WashWorkstation ws : mapper.selectBySite(siteId)) {
            if (!"Y".equals(ws.getEnabled())) {
                out.add(new WorkstationVO(ws.getWorkstationId(), ws.getSiteId(), ws.getCode(), ws.getName(),
                        "DISABLED", ws.getEnabled(), 0L, 0));
                continue;
            }
            List<WashSelfwashOrder> active = orderMapper.selectActiveByWorkstation(ws.getWorkstationId());
            WashSelfwashOrder serving = null;
            // 排队明细：服务中在前；预计等待 = 服务中剩余秒 + 各排队单总时长（加时已累进 addedSeconds，expireAt 动态刷新）
            List<WorkstationQueueItemVO> queueItems = new ArrayList<>();
            long waitSeconds = 0L;
            for (WashSelfwashOrder o : active) {
                if (SelfWashStatus.SERVING.name().equals(o.getStatus())) {
                    serving = o;
                } else if (SelfWashStatus.QUEUING.name().equals(o.getStatus())) {
                    long totalSec = orderSeconds(o);
                    waitSeconds += totalSec;
                    queueItems.add(new WorkstationQueueItemVO(maskPlate(o.getPlateNo()),
                            o.getServiceName(), SelfWashStatus.QUEUING.name(), (int) Math.ceil(totalSec / 60.0)));
                }
            }
            // 此时 queueItems 仅含排队中订单，其数量即 queueCount（服务中单随后才插入队首）
            int queue = queueItems.size();
            if (serving != null) {
                long remain = serving.getExpireAt() != null ? Math.max(0L, (serving.getExpireAt() - now) / 1000L) : 0L;
                waitSeconds += remain;
                queueItems.add(0, new WorkstationQueueItemVO(maskPlate(serving.getPlateNo()),
                        serving.getServiceName(), SelfWashStatus.SERVING.name(), (int) Math.ceil(remain / 60.0)));
                out.add(new WorkstationVO(ws.getWorkstationId(), ws.getSiteId(), ws.getCode(), ws.getName(),
                        "SERVING", ws.getEnabled(), remain, queue));
            } else {
                out.add(new WorkstationVO(ws.getWorkstationId(), ws.getSiteId(), ws.getCode(), ws.getName(),
                        "IDLE", ws.getEnabled(), 0L, queue));
            }
            WorkstationVO vo = out.get(out.size() - 1);
            vo.setEstimatedWaitMinutes((int) Math.ceil(waitSeconds / 60.0));
            vo.setQueueItems(queueItems);
        }
        return out;
    }

    /** 单笔订单总时长（秒）：付费时长 + 累计加时；加时即改 expireAt/addedSeconds，等待分钟随之动态变化。 */
    private long orderSeconds(WashSelfwashOrder o) {
        long minutes = o.getServiceMinutes() == null ? 0L : o.getServiceMinutes();
        long added = o.getAddedSeconds() == null ? 0L : o.getAddedSeconds();
        return minutes * 60L + added;
    }

    /**
     * 车牌脱敏：前 2 位 + *** + 后 2 位（如 京A***12）；过短或无车牌显示 ***。
     * 隐私：C 端不下发完整车牌与订单号。
     */
    private String maskPlate(String plateNo) {
        if (plateNo == null || plateNo.isBlank()) return "***";
        int len = plateNo.length();
        if (len <= 4) return "***";
        return plateNo.substring(0, 2) + "***" + plateNo.substring(len - 2);
    }

    private void requireExists(Long id) {
        if (id == null || mapper.selectById(id) == null) {
            throw new ApiException(ErrorCode.A0001, "工位不存在");
        }
    }
}
