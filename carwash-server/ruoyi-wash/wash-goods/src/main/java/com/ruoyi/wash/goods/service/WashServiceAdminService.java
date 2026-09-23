package com.ruoyi.wash.goods.service;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.goods.domain.WashService;
import com.ruoyi.wash.goods.mapper.WashServiceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 后台服务项管理（商品可配置）。
 *
 * <p>为什么需要：C 端展示的服务项此前只能靠改 SQL 维护，运营无法自助上下架/改价。
 * 本服务只做 CRUD，价格快照逻辑仍在下单侧（下单时复制服务名与价格，改价不影响历史订单）。
 */
@Service
public class WashServiceAdminService {

    @Autowired
    private WashServiceMapper serviceMapper;

    public List<WashService> list(String serviceName, Long categoryId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return serviceMapper.selectAdminList(serviceName, categoryId, offset, pageSize);
    }

    public long count(String serviceName, Long categoryId) {
        return serviceMapper.countAdmin(serviceName, categoryId);
    }

    public WashService get(Long serviceId) {
        WashService service = serviceMapper.selectById(serviceId);
        if (service == null) {
            throw new ApiException(ErrorCode.C1001, "服务项不存在");
        }
        return service;
    }

    /** 新增：名称必填、价格不能为负、工时为正常值，默认上架 */
    public Long add(WashService service) {
        check(service);
        if (StringUtils.isEmpty(service.getEnabled())) {
            service.setEnabled("Y");
        }
        serviceMapper.insertService(service);
        return service.getServiceId();
    }

    public void edit(WashService service) {
        if (service.getServiceId() == null) {
            throw new ApiException(ErrorCode.C1001, "缺少 serviceId");
        }
        if (serviceMapper.selectById(service.getServiceId()) == null) {
            throw new ApiException(ErrorCode.C1001, "服务项不存在");
        }
        check(service);
        serviceMapper.updateService(service);
    }

    public void remove(Long serviceId) {
        if (serviceMapper.selectById(serviceId) == null) {
            throw new ApiException(ErrorCode.C1001, "服务项不存在");
        }
        serviceMapper.logicDelete(serviceId);
    }

    /** 公共校验：与前端必填项一致，服务端必须再守一次（前端校验不是防线） */
    private void check(WashService service) {
        if (StringUtils.isEmpty(service.getServiceName())) {
            throw new ApiException(ErrorCode.C1001, "服务名称必填");
        }
        if (service.getPriceAmount() == null || service.getPriceAmount() < 0) {
            throw new ApiException(ErrorCode.C1001, "价格必填且不能为负（单位：分）");
        }
        if (service.getWorkMinutes() == null || service.getWorkMinutes() <= 0) {
            throw new ApiException(ErrorCode.C1001, "预计工时必填且大于 0");
        }
    }
}
