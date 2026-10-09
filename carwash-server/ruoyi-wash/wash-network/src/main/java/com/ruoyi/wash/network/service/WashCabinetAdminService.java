package com.ruoyi.wash.network.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.network.domain.WashCabinet;
import com.ruoyi.wash.network.domain.WashSlot;
import com.ruoyi.wash.network.mapper.WashCabinetMapper;
import com.ruoyi.wash.network.mapper.WashSlotMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/** 后台机柜（钥匙柜）管理：增删改查。格口管理见 WashSlotAdminService。 */
@Service
public class WashCabinetAdminService {

    @Autowired
    private WashCabinetMapper cabinetMapper;
    @Autowired
    private WashSlotMapper slotMapper;

    public List<WashCabinet> list(String keyword, Long communityId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return cabinetMapper.selectList(keyword, communityId, offset, pageSize);
    }

    public int count(String keyword, Long communityId) {
        return cabinetMapper.count(keyword, communityId);
    }

    public WashCabinet get(Long cabinetId) {
        return cabinetMapper.selectById(cabinetId);
    }

    public Long add(WashCabinet cabinet) {
        if (cabinet.getCabinetName() == null || cabinet.getCabinetName().isBlank()) {
            throw new ApiException(ErrorCode.A0001, "机柜名称必填");
        }
        if (cabinet.getSiteId() == null) {
            throw new ApiException(ErrorCode.A0001, "所属站点必选");
        }
        if (cabinet.getCommunityId() == null) {
            throw new ApiException(ErrorCode.A0001, "所在小区必选");
        }
        if (cabinet.getEnabled() == null || cabinet.getEnabled().isBlank()) {
            cabinet.setEnabled("Y");
        }
        cabinetMapper.insert(cabinet);
        return cabinet.getCabinetId();
    }

    public void edit(WashCabinet cabinet) {
        if (cabinet.getCabinetId() == null) {
            throw new ApiException(ErrorCode.A0001, "机柜ID必填");
        }
        cabinetMapper.update(cabinet);
    }

    public void remove(Long cabinetId) {
        if (slotMapper.countByCabinet(cabinetId) > 0) {
            throw new ApiException(ErrorCode.A0001, "该机柜下还有格口，请先删除格口后再删除机柜");
        }
        cabinetMapper.logicDelete(cabinetId);
    }
}
