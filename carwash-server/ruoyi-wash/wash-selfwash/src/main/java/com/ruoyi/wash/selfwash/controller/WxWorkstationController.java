package com.ruoyi.wash.selfwash.controller;

import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.selfwash.dto.WorkstationVO;
import com.ruoyi.wash.selfwash.service.WashWorkstationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端自助洗车工位。契约：openapi.yaml GET /api/v1/selfwash/workstations。
 */
@RestController
public class WxWorkstationController {

    @Autowired
    private WashWorkstationService service;

    @GetMapping("/api/v1/selfwash/workstations")
    public ApiResult<List<WorkstationVO>> list(@RequestParam Long siteId) {
        return ApiResult.ok(service.listWithStatus(siteId));
    }
}
