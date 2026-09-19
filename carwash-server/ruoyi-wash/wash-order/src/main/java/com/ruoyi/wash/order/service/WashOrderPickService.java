package com.ruoyi.wash.order.service;

import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.mapper.WashOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 师傅端订单查询（任务池数据源）。
 *
 * <p>只负责查数据（按站点 + 状态），VO 组装与业务校验在 wash-worker：
 * 订单域不感知师傅端的展示语义（PICKUP / RETURN 分组、照片要求等）。
 */
@Service
public class WashOrderPickService {

    @Autowired
    private WashOrderMapper orderMapper;

    public List<WashOrder> selectBySiteAndStatuses(Long siteId, List<String> statuses) {
        return orderMapper.selectBySiteAndStatuses(siteId, statuses);
    }
}
