package com.ruoyi.wash.member.controller;

import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wash.member.dto.AdminMemberVO;
import com.ruoyi.wash.member.dto.AdminVehicleVO;
import com.ruoyi.wash.member.service.WashMemberAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台会员 / 车辆查询。
 *
 * <p>权限标识遵循 PRD 6.7.3 的 wash:*:* 命名。
 * 手机号与 openid 只返回脱敏值（个保法最小化，技术方案 P8）。
 */
@RestController
@RequestMapping("/admin-api/wash")
public class AdminMemberController {

    @Autowired
    private WashMemberAdminService adminService;

    @PreAuthorize("@ss.hasPermi('wash:member:list')")
    @GetMapping("/member/list")
    public TableDataInfo memberList(@RequestParam(required = false) String keyword,
                                    @RequestParam(defaultValue = "1") int pageNum,
                                    @RequestParam(defaultValue = "10") int pageSize) {
        List<AdminMemberVO> list = adminService.listMembers(keyword, pageNum, pageSize);
        long total = adminService.countMembers(keyword);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        rspData.setTotal(total);
        return rspData;
    }

    @PreAuthorize("@ss.hasPermi('wash:vehicle:list')")
    @GetMapping("/vehicle/list")
    public TableDataInfo vehicleList(@RequestParam(required = false) String plateNo,
                                     @RequestParam(required = false) Long memberId,
                                     @RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "10") int pageSize) {
        List<AdminVehicleVO> list = adminService.listVehicles(plateNo, memberId, pageNum, pageSize);
        long total = adminService.countVehicles(plateNo, memberId);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        rspData.setTotal(total);
        return rspData;
    }
}
