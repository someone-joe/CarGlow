package com.ruoyi.wash.network.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.network.domain.WashCabinet;
import com.ruoyi.wash.network.mapper.WashCabinetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 机柜查询。跨模块只能调 Service。格口能力待柜机模块开工后在此扩展。 */
@Service
public class WashCabinetQueryService {

    @Autowired
    private WashCabinetMapper cabinetMapper;

    public WashCabinet requireEnabled(Long cabinetId) {
        WashCabinet cabinet = cabinetMapper.selectEnabledById(cabinetId);
        if (cabinet == null) {
            throw new ApiException(ErrorCode.A0001, "机柜不存在或已停用");
        }
        return cabinet;
    }
}
