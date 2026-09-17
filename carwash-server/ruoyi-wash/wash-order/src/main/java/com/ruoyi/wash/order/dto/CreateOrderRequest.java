package com.ruoyi.wash.order.dto;

import java.util.List;

/**
 * 下单请求 —— 与 openapi.yaml 的 CreateOrderRequest 逐字段一致，字段名不得另起。
 */
public record CreateOrderRequest(
        Long serviceId,
        Long vehicleId,
        Long cabinetId,
        String appointDate,
        String appointTime,
        Boolean pickupRequired,
        String remark,
        List<String> parkPhotoFileIds,
        Boolean agreed
) {
}
