package com.ruoyi.wash.order.dto;

import java.util.List;

/**
 * 订单列表返回 —— 与 openapi.yaml /api/v1/orders 响应 data 逐字段一致。
 * OrderListItemVO 内字段与契约 OrderListItemVO 一致；
 * mainAction / subActions 契约要求由后端返回，待下单功能接入后由按钮规则表驱动，当前恒为空。
 */
public class OrderPageVO {

    private List<OrderListItemVO> list;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;

    public static class OrderListItemVO {
        private String orderNo;
        private String status;
        private String statusLabel;
        private String serviceName;
        private Integer workMinutes;
        private Long appointTime;
        private String communityName;
        private String plateNo;
        private Long payAmount;
        private OrderActionVO mainAction;
        private List<OrderActionVO> subActions;

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatusLabel() {
            return statusLabel;
        }

        public void setStatusLabel(String statusLabel) {
            this.statusLabel = statusLabel;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public Integer getWorkMinutes() {
            return workMinutes;
        }

        public void setWorkMinutes(Integer workMinutes) {
            this.workMinutes = workMinutes;
        }

        public Long getAppointTime() {
            return appointTime;
        }

        public void setAppointTime(Long appointTime) {
            this.appointTime = appointTime;
        }

        public String getCommunityName() {
            return communityName;
        }

        public void setCommunityName(String communityName) {
            this.communityName = communityName;
        }

        public String getPlateNo() {
            return plateNo;
        }

        public void setPlateNo(String plateNo) {
            this.plateNo = plateNo;
        }

        public Long getPayAmount() {
            return payAmount;
        }

        public void setPayAmount(Long payAmount) {
            this.payAmount = payAmount;
        }

        public OrderActionVO getMainAction() {
            return mainAction;
        }

        public void setMainAction(OrderActionVO mainAction) {
            this.mainAction = mainAction;
        }

        public List<OrderActionVO> getSubActions() {
            return subActions;
        }

        public void setSubActions(List<OrderActionVO> subActions) {
            this.subActions = subActions;
        }
    }

    public List<OrderListItemVO> getList() {
        return list;
    }

    public void setList(List<OrderListItemVO> list) {
        this.list = list;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
