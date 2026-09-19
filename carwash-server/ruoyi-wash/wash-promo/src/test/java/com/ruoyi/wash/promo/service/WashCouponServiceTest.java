package com.ruoyi.wash.promo.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.promo.domain.WashCouponTemplate;
import com.ruoyi.wash.promo.domain.WashCouponUser;
import com.ruoyi.wash.promo.dto.CouponBestVO;
import com.ruoyi.wash.promo.dto.CouponTemplateVO;
import com.ruoyi.wash.promo.dto.CouponUserVO;
import com.ruoyi.wash.promo.mapper.WashCouponTemplateMapper;
import com.ruoyi.wash.promo.mapper.WashCouponUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("优惠券服务：领券中心 / 领取 / 最优券")
@ExtendWith(MockitoExtension.class)
class WashCouponServiceTest {

    @Mock
    private WashCouponTemplateMapper templateMapper;
    @Mock
    private WashCouponUserMapper userMapper;
    @InjectMocks
    private WashCouponService service;

    private static final long MEMBER = 123L;
    private static final long NOW = 1_700_000_000_000L;

    private WashCouponTemplate availableTemplate() {
        WashCouponTemplate t = new WashCouponTemplate();
        t.setCouponTemplateId(1L);
        t.setName("新客券");
        t.setType("NEWBIE");
        t.setThresholdAmount(0L);
        t.setDiscountAmount(500L);
        t.setTotal(1000);
        t.setIssued(1);
        t.setPerLimit(1);
        t.setStartTime(NOW - 1);
        t.setEndTime(NOW + 9_999_999_999_999L);
        t.setStatus("Y");
        return t;
    }

    @Test
    @DisplayName("领券中心：剩余库存=总量-已领，已领数来自统计")
    void center_calculatesRemainAndClaimed() {
        WashCouponTemplate t = availableTemplate();
        when(templateMapper.selectAvailable(anyLong())).thenReturn(List.of(t));
        when(userMapper.countByTemplateAndMember(1L, MEMBER)).thenReturn(3);

        List<CouponTemplateVO> vos = service.center(MEMBER, NOW);

        assertEquals(1, vos.size());
        CouponTemplateVO vo = vos.get(0);
        assertEquals("新客券", vo.name());
        assertEquals(999, vo.remain());   // 1000 - 1
        assertEquals(3, vo.claimed());
    }

    @Test
    @DisplayName("领取成功：校验通过后扣库存并落用户券")
    void receive_success() {
        WashCouponTemplate t = availableTemplate();
        when(templateMapper.selectById(1L)).thenReturn(t);
        when(userMapper.countByTemplateAndMember(1L, MEMBER)).thenReturn(0);
        when(templateMapper.incrementIssued(1L)).thenReturn(1);
        when(userMapper.insert(any())).thenReturn(1);

        CouponUserVO vo = service.receive(MEMBER, 1L, NOW);

        assertEquals("UNUSED", vo.status());
        assertEquals("新客券", vo.name());
        assertEquals(500L, vo.discountAmount());
        verify(templateMapper).incrementIssued(1L);
        verify(userMapper).insert(any(WashCouponUser.class));
    }

    @Test
    @DisplayName("领取失败：已下架/不在有效期抛 C1001")
    void receive_templateNotAvailable_throwsC1001() {
        WashCouponTemplate t = availableTemplate();
        t.setStatus("N");
        when(templateMapper.selectById(1L)).thenReturn(t);

        ApiException ex = assertThrows(ApiException.class, () -> service.receive(MEMBER, 1L, NOW));
        assertEquals(ErrorCode.C1001, ex.getErrorCode());
    }

    @Test
    @DisplayName("领取失败：超过每人限领抛 C1002")
    void receive_perLimitReached_throwsC1002() {
        WashCouponTemplate t = availableTemplate();
        when(templateMapper.selectById(1L)).thenReturn(t);
        when(userMapper.countByTemplateAndMember(1L, MEMBER)).thenReturn(t.getPerLimit());

        ApiException ex = assertThrows(ApiException.class, () -> service.receive(MEMBER, 1L, NOW));
        assertEquals(ErrorCode.C1002, ex.getErrorCode());
        verify(templateMapper, never()).incrementIssued(anyLong());
    }

    @Test
    @DisplayName("领取失败：库存扣减失败（已领完）抛 C1001")
    void receive_stockExhausted_throwsC1001() {
        WashCouponTemplate t = availableTemplate();
        when(templateMapper.selectById(1L)).thenReturn(t);
        when(userMapper.countByTemplateAndMember(1L, MEMBER)).thenReturn(0);
        when(templateMapper.incrementIssued(1L)).thenReturn(0);

        ApiException ex = assertThrows(ApiException.class, () -> service.receive(MEMBER, 1L, NOW));
        assertEquals(ErrorCode.C1001, ex.getErrorCode());
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("最优券：在可用券中选减免最大者，过期券被跳过")
    void best_choosesMaxDiscountAndSkipsExpired() {
        // 券1：门槛0 减500；券2：门槛1000 减300；券3：门槛0 减999 但已过期
        WashCouponTemplate t1 = availableTemplate();
        t1.setCouponTemplateId(1L);
        t1.setDiscountAmount(500L);

        WashCouponTemplate t2 = availableTemplate();
        t2.setCouponTemplateId(2L);
        t2.setThresholdAmount(1000L);
        t2.setDiscountAmount(300L);

        WashCouponTemplate t3 = availableTemplate();
        t3.setCouponTemplateId(3L);
        t3.setDiscountAmount(999L);

        WashCouponUser u1 = new WashCouponUser();
        u1.setCouponUserId(11L);
        u1.setTemplateId(1L);
        u1.setExpireTime(NOW + 1000);

        WashCouponUser u2 = new WashCouponUser();
        u2.setCouponUserId(22L);
        u2.setTemplateId(2L);
        u2.setExpireTime(NOW + 1000);

        WashCouponUser u3 = new WashCouponUser();
        u3.setCouponUserId(33L);
        u3.setTemplateId(3L);
        u3.setExpireTime(NOW - 1000); // 过期

        when(userMapper.selectMy(MEMBER, "UNUSED")).thenReturn(List.of(u1, u2, u3));
        when(templateMapper.selectById(1L)).thenReturn(t1);
        when(templateMapper.selectById(2L)).thenReturn(t2);

        CouponBestVO best = service.best(MEMBER, 2000L, NOW);
        assertNotNull(best);
        assertEquals(11L, best.couponUserId());
        assertEquals(500L, best.discountAmount()); // 未被过期的 999 抢走
    }

    @Test
    @DisplayName("最优券：门槛不满足时返回 null")
    void best_noneEligible_returnsNull() {
        WashCouponTemplate t1 = availableTemplate();
        t1.setCouponTemplateId(1L);
        t1.setThresholdAmount(100L);

        WashCouponUser u1 = new WashCouponUser();
        u1.setCouponUserId(11L);
        u1.setTemplateId(1L);
        u1.setExpireTime(NOW + 1000);

        when(userMapper.selectMy(MEMBER, "UNUSED")).thenReturn(List.of(u1));
        when(templateMapper.selectById(1L)).thenReturn(t1);

        assertNull(service.best(MEMBER, 50L, NOW)); // 50 < 门槛100
    }

    @Test
    @DisplayName("我的优惠券：按状态查询并映射模板名")
    void myCoupons_mapsTemplateName() {
        WashCouponTemplate t = availableTemplate();
        WashCouponUser u = new WashCouponUser();
        u.setCouponUserId(11L);
        u.setTemplateId(1L);
        u.setStatus("UNUSED");
        u.setExpireTime(NOW + 1000);

        when(userMapper.selectMy(MEMBER, "UNUSED")).thenReturn(List.of(u));
        when(templateMapper.selectById(1L)).thenReturn(t);

        List<CouponUserVO> vos = service.myCoupons(MEMBER, "UNUSED", NOW);
        assertEquals(1, vos.size());
        assertEquals("新客券", vos.get(0).name());
    }

    @Test
    @DisplayName("核销：透传 updateUsed")
    void markUsed_delegatesToMapper() {
        service.markUsed(9L, "SE123", NOW);
        verify(userMapper).updateUsed(9L, "SE123", NOW);
    }

    private WashCouponUser ownedCoupon(Long id, long templateId, String status, long expireTime) {
        WashCouponUser u = new WashCouponUser();
        u.setCouponUserId(id);
        u.setMemberId(MEMBER);
        u.setTemplateId(templateId);
        u.setStatus(status);
        u.setExpireTime(expireTime);
        return u;
    }

    @Test
    @DisplayName("下单用券：校验通过返回抵扣额并核销（门槛0减500）")
    void applyToOrder_success() {
        WashCouponTemplate t = availableTemplate(); // 门槛0 减500
        when(userMapper.selectById(11L)).thenReturn(ownedCoupon(11L, 1L, "UNUSED", NOW + 1000));
        when(templateMapper.selectById(1L)).thenReturn(t);

        long discount = service.applyToOrder(MEMBER, 11L, 3900L, "SE_ORDER", NOW);

        assertEquals(500L, discount);
        verify(userMapper).updateUsed(11L, "SE_ORDER", NOW);
    }

    @Test
    @DisplayName("下单用券：不属于本人或未找到抛 C1003")
    void applyToOrder_notOwned_throwsC1003() {
        when(userMapper.selectById(11L)).thenReturn(null);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.applyToOrder(MEMBER, 11L, 3900L, "SE_ORDER", NOW));
        assertEquals(ErrorCode.C1003, ex.getErrorCode());
    }

    @Test
    @DisplayName("下单用券：已使用抛 C1003，不重复核销")
    void applyToOrder_alreadyUsed_throwsC1003() {
        when(userMapper.selectById(11L)).thenReturn(ownedCoupon(11L, 1L, "USED", NOW + 1000));
        ApiException ex = assertThrows(ApiException.class,
                () -> service.applyToOrder(MEMBER, 11L, 3900L, "SE_ORDER", NOW));
        assertEquals(ErrorCode.C1003, ex.getErrorCode());
        verify(userMapper, never()).updateUsed(anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("下单用券：已过期抛 C1003")
    void applyToOrder_expired_throwsC1003() {
        when(userMapper.selectById(11L)).thenReturn(ownedCoupon(11L, 1L, "UNUSED", NOW - 1000));
        ApiException ex = assertThrows(ApiException.class,
                () -> service.applyToOrder(MEMBER, 11L, 3900L, "SE_ORDER", NOW));
        assertEquals(ErrorCode.C1003, ex.getErrorCode());
    }

    @Test
    @DisplayName("下单用券：订单金额不满足门槛抛 C1003")
    void applyToOrder_thresholdNotMet_throwsC1003() {
        WashCouponTemplate t = availableTemplate();
        t.setThresholdAmount(5000L); // 门槛 5000，订单仅 3900
        when(userMapper.selectById(11L)).thenReturn(ownedCoupon(11L, 1L, "UNUSED", NOW + 1000));
        when(templateMapper.selectById(1L)).thenReturn(t);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.applyToOrder(MEMBER, 11L, 3900L, "SE_ORDER", NOW));
        assertEquals(ErrorCode.C1003, ex.getErrorCode());
    }
}
