package com.ruoyi.wash.member.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.member.domain.WashParkLocation;
import com.ruoyi.wash.member.dto.ParkLocationVO;
import com.ruoyi.wash.member.mapper.WashParkLocationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 常用停车位置。成员自有数据，增删改查都按 MemberContext 的 memberId 隔离，禁止传 memberId 入参。
 */
@Service
public class WashParkLocationService {

    @Autowired
    private WashParkLocationMapper mapper;

    public List<ParkLocationVO> listByMember(Long memberId) {
        return mapper.selectByMember(memberId).stream().map(this::toVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(Long memberId, Long communityId, String communityName, String building,
                       String parkingNo, String detail, String isDefault) {
        if (communityId == null) {
            throw new ApiException(ErrorCode.A0001, "请选择所在小区");
        }
        WashParkLocation loc = new WashParkLocation();
        loc.setMemberId(memberId);
        loc.setCommunityId(communityId);
        loc.setCommunityName(communityName);
        loc.setBuilding(building);
        loc.setParkingNo(parkingNo);
        loc.setDetail(detail);
        String def = "Y".equalsIgnoreCase(isDefault) ? "Y" : "N";
        if ("Y".equals(def)) {
            // location_id 均 > 0，传 0 表示清空该会员所有默认
            mapper.clearOtherDefault(memberId, 0L);
        }
        loc.setIsDefault(def);
        mapper.insert(loc);
        return loc.getLocationId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long memberId, Long locationId, Long communityId, String communityName,
                       String building, String parkingNo, String detail, String isDefault) {
        requireOwned(memberId, locationId);
        WashParkLocation loc = new WashParkLocation();
        loc.setLocationId(locationId);
        loc.setMemberId(memberId);
        loc.setCommunityId(communityId);
        loc.setCommunityName(communityName);
        loc.setBuilding(building);
        loc.setParkingNo(parkingNo);
        loc.setDetail(detail);
        if (isDefault != null) {
            String def = "Y".equalsIgnoreCase(isDefault) ? "Y" : "N";
            if ("Y".equals(def)) {
                mapper.clearOtherDefault(memberId, locationId);
            }
            loc.setIsDefault(def);
        }
        mapper.update(loc);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long memberId, Long locationId) {
        requireOwned(memberId, locationId);
        mapper.softDelete(memberId, locationId);
    }

    private void requireOwned(Long memberId, Long locationId) {
        if (mapper.selectByMemberAndId(memberId, locationId) == null) {
            throw new ApiException(ErrorCode.A0001, "停车位置不存在");
        }
    }

    private ParkLocationVO toVO(WashParkLocation l) {
        return new ParkLocationVO(l.getLocationId(), l.getCommunityId(), l.getCommunityName(),
                l.getBuilding(), l.getParkingNo(), l.getDetail(), l.getIsDefault());
    }
}
