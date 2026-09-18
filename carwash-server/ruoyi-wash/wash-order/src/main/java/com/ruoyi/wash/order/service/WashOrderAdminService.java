package com.ruoyi.wash.order.service;

import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.dto.AdminOrderVO;
import com.ruoyi.wash.order.mapper.WashOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * 后台订单查询：跨用户的全量视图，仅供运营后台使用。
 * 与 C 端查询分开，避免"越权看到别人订单"这类问题在两套逻辑间互相污染。
 */
@Service
public class WashOrderAdminService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private WashOrderMapper orderMapper;

    /** memberId 用于"从会员查他的订单"（客服接到电话时的主路径） */
    public List<AdminOrderVO> list(String orderNo, String status, Long memberId, int pageNum, int pageSize) {
        int size = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int num = Math.max(pageNum, 1);
        return orderMapper
                .selectAdminPage(orderNo, status, memberId, (num - 1) * size, size)
                .stream()
                .map(this::toVO)
                .toList();
    }

    public long count(String orderNo, String status, Long memberId) {
        return orderMapper.countAdmin(orderNo, status, memberId);
    }

    private AdminOrderVO toVO(WashOrder order) {
        AdminOrderVO vo = new AdminOrderVO();
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusLabel(OrderStatus.of(order.getStatus()).getLabel());
        vo.setServiceName(order.getServiceName());
        vo.setPlateNo(order.getPlateNo());
        vo.setPayAmount(order.getPayAmount());
        vo.setMemberId(order.getMemberId());
        vo.setSiteId(order.getSiteId());
        vo.setCommunityId(order.getCommunityId());
        vo.setAppointTime(order.getAppointTime());
        vo.setCreateTime(order.getCreateTime() == null ? null
                : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(order.getCreateTime()));
        return vo;
    }
}
