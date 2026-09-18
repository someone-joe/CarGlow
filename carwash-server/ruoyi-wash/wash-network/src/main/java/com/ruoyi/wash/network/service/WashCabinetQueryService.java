package com.ruoyi.wash.network.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.network.domain.WashCabinet;
import com.ruoyi.wash.network.dto.CabinetVO;
import com.ruoyi.wash.network.mapper.WashCabinetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 机柜查询。跨模块只能调 Service。 */
@Service
public class WashCabinetQueryService {

    private static final String ENABLED = "Y";

    @Autowired
    private WashCabinetMapper cabinetMapper;

    @Autowired
    private WashSlotService slotService;

    public WashCabinet requireEnabled(Long cabinetId) {
        WashCabinet cabinet = cabinetMapper.selectEnabledById(cabinetId);
        if (cabinet == null) {
            throw new ApiException(ErrorCode.A0001, "机柜不存在或已停用");
        }
        return cabinet;
    }

    /**
     * C 端可选机柜列表：带空闲格口数，让用户下单前就看到哪个柜子还有位置。
     *
     * <p>格口数读 wash_slot 实时统计，不缓存：预占/释放都是写操作，缓存会误导用户选到已满的柜子。
     */
    public List<CabinetVO> listAvailable(Long communityId, String keyword) {
        List<CabinetVO> list = new ArrayList<>();
        for (WashCabinet cabinet : cabinetMapper.selectEnabledList(communityId, keyword)) {
            long total = slotService.countTotal(cabinet.getCabinetId());
            long free = slotService.countFree(cabinet.getCabinetId());
            CabinetVO vo = new CabinetVO();
            vo.setCabinetId(cabinet.getCabinetId());
            vo.setName(cabinet.getCabinetName());
            vo.setSlotTotal((int) total);
            vo.setSlotFree((int) free);
            vo.setOnline(ENABLED.equals(cabinet.getEnabled()));
            vo.setFull(free <= 0);
            list.add(vo);
        }
        return list;
    }
}
