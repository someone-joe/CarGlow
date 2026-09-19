package com.ruoyi.wash.promo.controller;

import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.promo.dto.CouponBestVO;
import com.ruoyi.wash.promo.dto.CouponTemplateVO;
import com.ruoyi.wash.promo.dto.CouponUserVO;
import com.ruoyi.wash.promo.service.WashCouponService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

@DisplayName("接入层：C 端优惠券控制器（MemberContext 透传 + 委托）")
@ExtendWith(MockitoExtension.class)
class WxCouponControllerTest {

    @Mock
    private WashCouponService couponService;
    @InjectMocks
    private WxCouponController controller;

    private static final long MEMBER = 123L;

    @BeforeEach
    void setUp() {
        MemberContext.set(MEMBER);
    }

    @AfterEach
    void tearDown() {
        MemberContext.clear();
    }

    @Test
    @DisplayName("领券中心：返回 ok 并透传 memberId")
    void center_delegates() {
        when(couponService.center(eq(MEMBER), anyLong())).thenReturn(List.of());
        ApiResult<List<CouponTemplateVO>> r = controller.center();
        assertEquals(0, r.getCode());
        verify(couponService).center(eq(MEMBER), anyLong());
    }

    @Test
    @DisplayName("领取：返回 ok 并透传模板号与 memberId")
    void receive_delegates() {
        CouponUserVO vo = new CouponUserVO(2L, 2L, "满减券", 3900L, 300L, "UNUSED", 0L, null);
        when(couponService.receive(eq(MEMBER), eq(2L), anyLong())).thenReturn(vo);
        ApiResult<CouponUserVO> r = controller.receive(2L);
        assertEquals(0, r.getCode());
        assertSame(vo, r.getData());
        verify(couponService).receive(eq(MEMBER), eq(2L), anyLong());
    }

    @Test
    @DisplayName("我的优惠券：透传 status 过滤")
    void my_delegates() {
        when(couponService.myCoupons(eq(MEMBER), eq("UNUSED"), anyLong())).thenReturn(List.of());
        ApiResult<List<CouponUserVO>> r = controller.my("UNUSED");
        assertEquals(0, r.getCode());
        verify(couponService).myCoupons(eq(MEMBER), eq("UNUSED"), anyLong());
    }

    @Test
    @DisplayName("最优券：透传支付金额")
    void best_delegates() {
        CouponBestVO best = new CouponBestVO(11L, 500L);
        when(couponService.best(eq(MEMBER), eq(3900L), anyLong())).thenReturn(best);
        ApiResult<CouponBestVO> r = controller.best(3900L);
        assertEquals(0, r.getCode());
        assertSame(best, r.getData());
        verify(couponService).best(eq(MEMBER), eq(3900L), anyLong());
    }
}
