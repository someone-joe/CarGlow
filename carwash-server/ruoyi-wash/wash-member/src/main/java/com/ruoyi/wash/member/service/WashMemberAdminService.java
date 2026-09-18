package com.ruoyi.wash.member.service;

import com.ruoyi.wash.member.domain.WashMember;
import com.ruoyi.wash.member.domain.WashVehicle;
import com.ruoyi.wash.member.dto.AdminMemberVO;
import com.ruoyi.wash.member.dto.AdminVehicleVO;
import com.ruoyi.wash.member.mapper.WashMemberMapper;
import com.ruoyi.wash.member.mapper.WashVehicleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * 后台会员/车辆查询。
 *
 * <p>对外一律返回脱敏字段（手机号、openid），明文不出本服务（个保法最小化，技术方案 P8）。
 */
@Service
public class WashMemberAdminService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private WashMemberMapper memberMapper;

    @Autowired
    private WashVehicleMapper vehicleMapper;

    public List<AdminMemberVO> listMembers(String keyword, int pageNum, int pageSize) {
        int size = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int num = Math.max(pageNum, 1);
        return memberMapper.selectAdminPage(keyword, (num - 1) * size, size)
                .stream()
                .map(this::toMemberVO)
                .toList();
    }

    public long countMembers(String keyword) {
        return memberMapper.countAdmin(keyword);
    }

    public List<AdminVehicleVO> listVehicles(String plateNo, Long memberId, int pageNum, int pageSize) {
        int size = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int num = Math.max(pageNum, 1);
        return vehicleMapper.selectAdminPage(plateNo, memberId, (num - 1) * size, size)
                .stream()
                .map(this::toVehicleVO)
                .toList();
    }

    public long countVehicles(String plateNo, Long memberId) {
        return vehicleMapper.countAdmin(plateNo, memberId);
    }

    private AdminMemberVO toMemberVO(WashMember member) {
        AdminMemberVO vo = new AdminMemberVO();
        vo.setMemberId(member.getMemberId());
        vo.setOpenidMasked(mask(member.getOpenid(), 4, 4));
        vo.setNickname(member.getNickname());
        vo.setPhoneMasked(maskPhone(member.getPhone()));
        vo.setCreateTime(member.getCreateTime() == null ? null
                : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(member.getCreateTime()));
        return vo;
    }

    private AdminVehicleVO toVehicleVO(WashVehicle vehicle) {
        AdminVehicleVO vo = new AdminVehicleVO();
        vo.setVehicleId(vehicle.getVehicleId());
        vo.setMemberId(vehicle.getMemberId());
        vo.setPlateNo(vehicle.getPlateNo());
        vo.setBrand(vehicle.getBrand());
        vo.setColor(vehicle.getColor());
        vo.setIsNewEnergy(vehicle.getIsNewEnergy());
        vo.setCommunityId(vehicle.getCommunityId());
        vo.setParkingNo(vehicle.getParkingNo());
        vo.setCreateTime(vehicle.getCreateTime() == null ? null
                : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(vehicle.getCreateTime()));
        return vo;
    }

    /** 手机号脱敏：138****1234 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone == null ? null : "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 通用脱敏：保留前 head 位与后 tail 位 */
    private String mask(String value, int head, int tail) {
        if (value == null) {
            return null;
        }
        if (value.length() <= head + tail) {
            return "****";
        }
        return value.substring(0, head) + "****" + value.substring(value.length() - tail);
    }
}
