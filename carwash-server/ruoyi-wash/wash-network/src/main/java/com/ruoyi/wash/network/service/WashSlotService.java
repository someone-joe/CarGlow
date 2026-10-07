package com.ruoyi.wash.network.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.network.domain.WashSlot;
import com.ruoyi.wash.network.mapper.WashSlotMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 格口服务：预占 / 占用 / 释放 / 开箱留痕。
 *
 * <p>并发安全：一律用「where 当前状态 = 期望状态」的乐观更新，更新行数为 0 即被别人抢走，
 * 不会出现两个订单占同一个格口（这是最容易超卖的地方）。
 */
@Service
public class WashSlotService {

    public static final String FREE = "FREE";
    public static final String RESERVED = "RESERVED";
    public static final String OCCUPIED = "OCCUPIED";

    /** 开箱角色：工作人员（PRD 6.2 三类角色：客户 / 工作人员 / 管理员） */
    public static final String ROLE_WORKER = "WORKER";

    /** 来源端：师傅小程序 */
    public static final String SOURCE_WORKER = "WORKER";

    @Autowired
    private WashSlotMapper slotMapper;

    /**
     * 下单时预占格口。
     *
     * @return 预占到的格口
     * @throws ApiException B2003 无空闲格口
     */
    public WashSlot reserve(Long cabinetId, String orderNo, Long memberId) {
        List<WashSlot> free = slotMapper.selectFree(cabinetId, 5);
        for (WashSlot slot : free) {
            int rows = slotMapper.updateStatus(slot.getSlotId(), FREE, RESERVED, orderNo, memberId);
            if (rows > 0) {
                slotMapper.insertOpenLog(slot.getCabinetId(), slot.getSlotId(), slot.getSlotNo(),
                        orderNo, memberId, "RESERVE", null);
                WashSlot reserved = slotMapper.selectById(slot.getSlotId());
                return reserved;
            }
        }
        throw new ApiException(ErrorCode.B2003);
    }

    /** 客户存入钥匙：预占 → 占用 */
    public WashSlot occupy(String orderNo) {
        WashSlot slot = require(orderNo);
        slotMapper.updateStatus(slot.getSlotId(), RESERVED, OCCUPIED, orderNo, slot.getMemberId());
        slotMapper.insertOpenLog(slot.getCabinetId(), slot.getSlotId(), slot.getSlotNo(),
                orderNo, slot.getMemberId(), "DEPOSIT", null);
        return slotMapper.selectById(slot.getSlotId());
    }

    /** 释放格口（取消、取回钥匙、异常作废）：调用方没有操作者信息时留痕不带角色 */
    public void release(String orderNo) {
        release(orderNo, null, null, null);
    }

    /**
     * 带操作者的释放：师傅取走钥匙后必须走这个重载，
     * 否则留痕只记 member_id，会把师傅的动作记到会员头上。
     */
    public void release(String orderNo, String role, Long userId, String source) {
        WashSlot slot = slotMapper.selectByOrderNo(orderNo);
        slotMapper.releaseByOrderNo(orderNo);
        if (slot != null) {
            slotMapper.insertOpenLogWithActor(slot.getCabinetId(), slot.getSlotId(), slot.getSlotNo(),
                    orderNo, slot.getMemberId(), "RELEASE", null, role, userId, source, "SUCCESS");
        }
    }

    public WashSlot require(String orderNo) {
        WashSlot slot = slotMapper.selectByOrderNo(orderNo);
        if (slot == null) {
            throw new ApiException(ErrorCode.B2001, "柜机离线或格口不可用");
        }
        return slot;
    }

    /** 机柜格口总数 */
    public long countTotal(Long cabinetId) {
        return slotMapper.countByCabinet(cabinetId);
    }

    /** 机柜空闲格口数 */
    public long countFree(Long cabinetId) {
        return slotMapper.countFreeByCabinet(cabinetId);
    }

    /** 取开箱码时留痕（开箱码脱敏后记录，不明文落库） */
    public void logOpenCode(WashSlot slot, String openCode) {
        String masked = openCode == null ? null : openCode.substring(0, Math.min(2, openCode.length())) + "****";
        slotMapper.insertOpenLog(slot.getCabinetId(), slot.getSlotId(), slot.getSlotNo(),
                slot.getOrderNo(), slot.getMemberId(), "OPEN_CODE", masked);
    }

    /**
     * 带操作者的开箱留痕（角色 / 人员 / 来源端 / 结果）。
     *
     * <p>与 logOpenCode 的区别：后者只有会员维度（客户取码）。
     * 工作人员 / 管理员开箱一律走本方法，否则留痕里查不到真正开箱的人（PRD 6.2 留痕完整率 100%）。
     */
    public void logOpen(WashSlot slot, String action, String openCode, String role, Long userId, String source, String result) {
        String masked = openCode == null ? null : openCode.substring(0, Math.min(2, openCode.length())) + "****";
        slotMapper.insertOpenLogWithActor(slot.getCabinetId(), slot.getSlotId(), slot.getSlotNo(),
                slot.getOrderNo(), slot.getMemberId(), action, masked, role, userId, source, result);
    }
}
