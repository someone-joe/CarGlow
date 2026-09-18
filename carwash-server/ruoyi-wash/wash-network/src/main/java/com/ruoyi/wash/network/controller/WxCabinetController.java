package com.ruoyi.wash.network.controller;

import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.network.dto.CabinetVO;
import com.ruoyi.wash.network.service.WashCabinetQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端机柜。契约：openapi.yaml GET /api/v1/cabinets。
 *
 * <p>契约里的 lng / lat 暂不接收：距离计算需要机柜坐标与用户定位授权，均未开工，
 * distance 一律返回 null，前端不要展示"距您 xx 米"。
 */
@RestController
public class WxCabinetController {

    @Autowired
    private WashCabinetQueryService cabinetQuery;

    @GetMapping("/api/v1/cabinets")
    public ApiResult<List<CabinetVO>> list(@RequestParam(required = false) Long communityId,
                                           @RequestParam(required = false) String keyword) {
        return ApiResult.ok(cabinetQuery.listAvailable(communityId, keyword));
    }
}
