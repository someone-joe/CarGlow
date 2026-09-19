package com.ruoyi.wash.promo.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.promo.domain.WashInsurancePolicy;
import com.ruoyi.wash.promo.domain.WashInsuranceProduct;
import com.ruoyi.wash.promo.dto.InsurancePolicyVO;
import com.ruoyi.wash.promo.dto.InsuranceProductVO;
import com.ruoyi.wash.promo.mapper.WashInsurancePolicyMapper;
import com.ruoyi.wash.promo.mapper.WashInsuranceProductMapper;
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

@DisplayName("保险服务：产品列表 / 投保 / 我的保单")
@ExtendWith(MockitoExtension.class)
class WashInsuranceServiceTest {

    @Mock
    private WashInsuranceProductMapper productMapper;
    @Mock
    private WashInsurancePolicyMapper policyMapper;
    @InjectMocks
    private WashInsuranceService service;

    private static final long MEMBER = 123L;
    private static final long NOW = 1_700_000_000_000L;
    private static final long TERM_MS = 365L * 24 * 3600 * 1000;

    private WashInsuranceProduct enabledProduct() {
        WashInsuranceProduct p = new WashInsuranceProduct();
        p.setInsuranceProductId(1L);
        p.setName("车漆保障险");
        p.setPriceAmount(990L);
        p.setCoverageDesc("单次最高赔付 2000 元");
        p.setStatus("Y");
        return p;
    }

    @Test
    @DisplayName("产品列表：仅返回上架产品")
    void products_returnsEnabledOnly() {
        WashInsuranceProduct p = enabledProduct();
        when(productMapper.selectEnabled()).thenReturn(List.of(p));

        List<InsuranceProductVO> vos = service.products();
        assertEquals(1, vos.size());
        assertEquals("车漆保障险", vos.get(0).name());
        assertEquals(990L, vos.get(0).priceAmount());
    }

    @Test
    @DisplayName("投保成功：生成 ACTIVE 保单，期限 365 天，实付=保费")
    void buy_success() {
        WashInsuranceProduct p = enabledProduct();
        when(productMapper.selectById(1L)).thenReturn(p);
        when(policyMapper.insert(any())).thenReturn(1);

        InsurancePolicyVO vo = service.buy(MEMBER, 1L, 9L, null, NOW);

        assertEquals("ACTIVE", vo.status());
        assertEquals(990L, vo.paidAmount());
        assertEquals(9L, vo.vehicleId());
        assertEquals(1L, vo.productId());
        assertTrue(vo.policyNo().startsWith("BX"));
        assertEquals(TERM_MS, vo.endTime() - vo.startTime());
        verify(policyMapper).insert(any(WashInsurancePolicy.class));
    }

    @Test
    @DisplayName("投保失败：车辆为空抛 C2002")
    void buy_nullVehicle_throwsC2002() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.buy(MEMBER, 1L, null, null, NOW));
        assertEquals(ErrorCode.C2002, ex.getErrorCode());
        verify(policyMapper, never()).insert(any());
    }

    @Test
    @DisplayName("投保失败：产品不存在或下架抛 C2001")
    void buy_productInvalid_throwsC2001() {
        // 不存在
        when(productMapper.selectById(1L)).thenReturn(null);
        ApiException ex1 = assertThrows(ApiException.class,
                () -> service.buy(MEMBER, 1L, 9L, null, NOW));
        assertEquals(ErrorCode.C2001, ex1.getErrorCode());

        // 已下架
        WashInsuranceProduct off = enabledProduct();
        off.setStatus("N");
        when(productMapper.selectById(1L)).thenReturn(off);
        ApiException ex2 = assertThrows(ApiException.class,
                () -> service.buy(MEMBER, 1L, 9L, null, NOW));
        assertEquals(ErrorCode.C2001, ex2.getErrorCode());
    }

    @Test
    @DisplayName("我的保单：映射产品名与实付")
    void myPolicies_mapsProduct() {
        WashInsuranceProduct p = enabledProduct();
        WashInsurancePolicy pol = new WashInsurancePolicy();
        pol.setPolicyId(1L);
        pol.setPolicyNo("BX17000000000001");
        pol.setProductId(1L);
        pol.setVehicleId(9L);
        pol.setOrderNo(null);
        pol.setStatus("ACTIVE");
        pol.setStartTime(NOW);
        pol.setEndTime(NOW + TERM_MS);
        pol.setPaidAmount(990L);

        when(policyMapper.selectMy(MEMBER)).thenReturn(List.of(pol));
        when(productMapper.selectById(1L)).thenReturn(p);

        List<InsurancePolicyVO> vos = service.myPolicies(MEMBER);
        assertEquals(1, vos.size());
        assertEquals("车漆保障险", vos.get(0).name());
        assertEquals(990L, vos.get(0).paidAmount());
    }
}
