package com.ruoyi.wash.network.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.network.domain.WashSlot;
import com.ruoyi.wash.network.mapper.WashSlotMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** 后台格口管理：增删改查 + 开箱留痕审计。 */
@Service
public class WashSlotAdminService {

    @Autowired
    private WashSlotMapper slotMapper;

    public List<WashSlot> list(Long cabinetId, String status, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return slotMapper.selectList(cabinetId, status, offset, pageSize);
    }

    public int count(Long cabinetId, String status) {
        return slotMapper.count(cabinetId, status);
    }

    public WashSlot get(Long slotId) {
        return slotMapper.selectById(slotId);
    }

    public Long add(WashSlot slot) {
        if (slot.getCabinetId() == null) {
            throw new ApiException(ErrorCode.A0001, "所属机柜必选");
        }
        if (slot.getSlotNo() == null || slot.getSlotNo().isBlank()) {
            throw new ApiException(ErrorCode.A0001, "格口号必填");
        }
        if (slotMapper.countByCabinetAndSlotNo(slot.getCabinetId(), slot.getSlotNo()) > 0) {
            throw new ApiException(ErrorCode.A0001, "该机柜下已存在此格口号");
        }
        if (slot.getStatus() == null || slot.getStatus().isBlank()) {
            slot.setStatus("FREE");
        }
        slotMapper.insert(slot);
        return slot.getSlotId();
    }

    public void edit(WashSlot slot) {
        if (slot.getSlotId() == null) {
            throw new ApiException(ErrorCode.A0001, "格口ID必填");
        }
        slotMapper.update(slot);
    }

    public void remove(Long slotId) {
        WashSlot slot = slotMapper.selectById(slotId);
        if (slot != null && ("RESERVED".equals(slot.getStatus()) || "OCCUPIED".equals(slot.getStatus()))) {
            throw new ApiException(ErrorCode.A0001, "格口正在使用中（预占/占用），不可删除");
        }
        slotMapper.logicDelete(slotId);
    }

    public List<Map<String, Object>> listOpenLog(Long cabinetId, Long slotId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return slotMapper.selectOpenLog(cabinetId, slotId, offset, pageSize);
    }

    public int countOpenLog(Long cabinetId, Long slotId) {
        return slotMapper.countOpenLog(cabinetId, slotId);
    }
}
