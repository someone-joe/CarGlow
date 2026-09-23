package com.ruoyi.wash.goods.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.goods.domain.WashService;
import com.ruoyi.wash.goods.domain.WashServiceCategory;
import com.ruoyi.wash.goods.dto.ServiceCategoryVO;
import com.ruoyi.wash.goods.dto.ServiceDetailVO;
import com.ruoyi.wash.goods.dto.ServiceVO;
import com.ruoyi.wash.goods.mapper.WashServiceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 服务项查询。跨模块只能调 Service。 */
@Service
public class WashServiceQueryService {

    /**
     * 取送车费：平台卖点是免费取送，契约要求 C 端固定展示 0（openapi ServiceVO.pickupFee）。
     * 将来若改成收费，只改这里，不要散到各页面。
     */
    private static final long PICKUP_FEE_FREE = 0L;

    @Autowired
    private WashServiceMapper serviceMapper;

    public WashService requireEnabled(Long serviceId) {
        WashService service = serviceMapper.selectEnabledById(serviceId);
        if (service == null) {
            throw new ApiException(ErrorCode.A0001, "服务项不存在或已下架");
        }
        return service;
    }

    /** C 端可下单的服务项列表 */
    public List<ServiceVO> listEnabled() {
        List<ServiceVO> list = new ArrayList<>();
        for (WashService service : serviceMapper.selectEnabledList()) {
            ServiceVO vo = new ServiceVO();
            vo.setServiceId(service.getServiceId());
            vo.setCategoryId(service.getCategoryId());
            vo.setName(service.getServiceName());
            vo.setDisplayPrice(service.getPriceAmount());
            vo.setWorkMinutes(service.getWorkMinutes());
            vo.setPickupFee(PICKUP_FEE_FREE);
            list.add(vo);
        }
        return list;
    }

    /** C 端服务详情（bug092408：此前接口未实现，详情页直接 404）。
     *  明细类字段（items/notices/样图等）待后台配置能力落地后返回，当前为空，前端有空态。 */
    public ServiceDetailVO detail(Long serviceId) {
        WashService service = requireEnabled(serviceId);
        ServiceDetailVO vo = new ServiceDetailVO();
        vo.setServiceId(service.getServiceId());
        vo.setCategoryId(service.getCategoryId());
        vo.setName(service.getServiceName());
        vo.setDisplayPrice(service.getPriceAmount());
        vo.setWorkMinutes(service.getWorkMinutes());
        vo.setPickupFee(PICKUP_FEE_FREE);
        return vo;
    }

    /** C 端服务分类 */
    public List<ServiceCategoryVO> listCategories() {
        List<ServiceCategoryVO> list = new ArrayList<>();
        for (WashServiceCategory category : serviceMapper.selectCategories()) {
            ServiceCategoryVO vo = new ServiceCategoryVO();
            vo.setCategoryId(category.getCategoryId());
            vo.setName(category.getCategoryName());
            vo.setSort(category.getSortNum());
            list.add(vo);
        }
        return list;
    }
}
