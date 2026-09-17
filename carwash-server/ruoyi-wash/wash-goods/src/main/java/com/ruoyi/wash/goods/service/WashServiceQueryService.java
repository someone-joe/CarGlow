package com.ruoyi.wash.goods.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.goods.domain.WashService;
import com.ruoyi.wash.goods.mapper.WashServiceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 服务项查询。跨模块只能调 Service。 */
@Service
public class WashServiceQueryService {

    @Autowired
    private WashServiceMapper serviceMapper;

    public WashService requireEnabled(Long serviceId) {
        WashService service = serviceMapper.selectEnabledById(serviceId);
        if (service == null) {
            throw new ApiException(ErrorCode.A0001, "服务项不存在或已下架");
        }
        return service;
    }
}
