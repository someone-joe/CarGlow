package com.ruoyi.wash.order.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.mapper.WashOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 供支付域（wash-pay）读取订单的服务。
 *
 * <p>跨域只暴露必要能力，避免支付域直接碰 Mapper 或状态机内部。
 */
@Service
public class WashOrderPaySupportService {

    @Autowired
    private WashOrderMapper orderMapper;

    /** 客户本人发起支付：校验归属 */
    public WashOrder requireOwned(String orderNo, Long memberId) {
        WashOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || !order.getMemberId().equals(memberId)) {
            throw new ApiException(ErrorCode.B1001);
        }
        return order;
    }

    /** 系统场景（支付回调、退款重试）：无登录态，按订单号查 */
    public WashOrder require(String orderNo) {
        WashOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new ApiException(ErrorCode.B1001);
        }
        return order;
    }
}
