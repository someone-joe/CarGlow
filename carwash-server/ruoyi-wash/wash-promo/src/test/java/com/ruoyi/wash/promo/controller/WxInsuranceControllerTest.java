package com.ruoyi.wash.promo.controller;

import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.promo.dto.BuyInsuranceRequest;
import com.ruoyi.wash.promo.dto.InsurancePolicyVO;
import com.ruoyi.wash.promo.dto.InsuranceProductVO;
import com.ruoyi.wash.promo.service.WashInsuranceService;
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

@DisplayName("接入层：C 端保险/保单控制器（MemberContext 透传 + 委托）")
@ExtendWith(MockitoExtension.class)
class WxInsuranceControllerTest {

    @Mock
    private WashInsuranceService insuranceService;
    @InjectMocks
    private WxInsuranceController controller;

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
    @DisplayName("产品列表：返回 ok")
    void products_delegates() {
        when(insuranceService.products()).thenReturn(List.of());
        ApiResult<List<InsuranceProductVO>> r = controller.products();
        assertEquals(0, r.getCode());
        verify(insuranceService).products();
    }

    @Test
    @DisplayName("投保：透传请求字段与 memberId")
    void buy_delegates() {
        BuyInsuranceRequest req = new BuyInsuranceRequest(1L, 9L, null);
        InsurancePolicyVO vo = new InsurancePolicyVO(2L, "BX1", 1L, "车漆保障险",
                9L, null, "ACTIVE", 1L, 2L, 990L);
        when(insuranceService.buy(eq(MEMBER), eq(1L), eq(9L), isNull(), anyLong())).thenReturn(vo);
        ApiResult<InsurancePolicyVO> r = controller.buy(req);
        assertEquals(0, r.getCode());
        assertSame(vo, r.getData());
        verify(insuranceService).buy(eq(MEMBER), eq(1L), eq(9L), isNull(), anyLong());
    }

    @Test
    @DisplayName("我的保单：返回 ok 并透传 memberId")
    void my_delegates() {
        when(insuranceService.myPolicies(eq(MEMBER))).thenReturn(List.of());
        ApiResult<List<InsurancePolicyVO>> r = controller.my();
        assertEquals(0, r.getCode());
        verify(insuranceService).myPolicies(eq(MEMBER));
    }
}
