package com.ruoyi.wash.selfwash.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.selfwash.domain.SelfWashStatus;
import com.ruoyi.wash.selfwash.domain.WashSelfwashOrder;
import com.ruoyi.wash.selfwash.domain.WashWorkstation;
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

    /** C 端工位列表（带实时状态：服务中剩余秒 / 排队人数）。siteId 由前端传当前站点。 */
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
            int queue = 0;
            for (WashSelfwashOrder o : active) {
                if (SelfWashStatus.SERVING.name().equals(o.getStatus())) {
                    serving = o;
                } else if (SelfWashStatus.QUEUING.name().equals(o.getStatus())) {
                    queue++;
                }
            }
            if (serving != null) {
                long remain = serving.getExpireAt() != null ? Math.max(0L, (serving.getExpireAt() - now) / 1000L) : 0L;
                out.add(new WorkstationVO(ws.getWorkstationId(), ws.getSiteId(), ws.getCode(), ws.getName(),
                        "SERVING", ws.getEnabled(), remain, queue));
            } else {
                out.add(new WorkstationVO(ws.getWorkstationId(), ws.getSiteId(), ws.getCode(), ws.getName(),
                        "IDLE", ws.getEnabled(), 0L, queue));
            }
        }
        return out;
    }

    private void requireExists(Long id) {
        if (id == null || mapper.selectById(id) == null) {
            throw new ApiException(ErrorCode.A0001, "工位不存在");
        }
    }
}
