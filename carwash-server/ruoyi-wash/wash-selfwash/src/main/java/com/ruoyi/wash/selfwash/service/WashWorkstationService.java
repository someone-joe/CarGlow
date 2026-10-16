package com.ruoyi.wash.selfwash.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.selfwash.domain.WashWorkstation;
import com.ruoyi.wash.selfwash.dto.WorkstationVO;
import com.ruoyi.wash.selfwash.mapper.WashWorkstationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 自助洗车工位。
 *
 * <p>实时状态（空闲/服务中/排队）由自助订单推导：Stage 1 无自助订单，故启用即 IDLE、停用即 DISABLED；
 * Stage 2 引入自助订单后，在此结合进行中订单给出 SERVING 及排队信息（队列人数、前方车牌）。
 */
@Service
public class WashWorkstationService {

    @Autowired
    private WashWorkstationMapper mapper;

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

    /** C 端工位列表（带实时状态）。siteId 由前端传当前站点。 */
    public List<WorkstationVO> listWithStatus(Long siteId) {
        List<WorkstationVO> out = new ArrayList<>();
        for (WashWorkstation ws : mapper.selectBySite(siteId)) {
            String status = "Y".equals(ws.getEnabled()) ? "IDLE" : "DISABLED";
            out.add(new WorkstationVO(ws.getWorkstationId(), ws.getSiteId(), ws.getCode(), ws.getName(), status, ws.getEnabled()));
        }
        return out;
    }

    private void requireExists(Long id) {
        if (id == null || mapper.selectById(id) == null) {
            throw new ApiException(ErrorCode.A0001, "工位不存在");
        }
    }
}
