package com.ruoyi.wash.order.state;

import com.ruoyi.wash.common.statemachine.OrderStateMachine;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.order.mapper.WashOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 状态机读写订单状态：乐观更新（where status = 原状态），并发改过时 affected = 0。
 */
@Component
public class WashOrderStatusAccessor implements OrderStateMachine.Accessor {

    @Autowired
    private WashOrderMapper orderMapper;

    @Override
    public OrderStatus loadStatus(String orderNo) {
        String status = orderMapper.selectStatusByOrderNo(orderNo);
        return status == null ? null : OrderStatus.of(status);
    }

    @Override
    public boolean updateStatus(String orderNo, OrderStatus from, OrderStatus to) {
        return orderMapper.updateStatus(orderNo, from.name(), to.name()) > 0;
    }
}
