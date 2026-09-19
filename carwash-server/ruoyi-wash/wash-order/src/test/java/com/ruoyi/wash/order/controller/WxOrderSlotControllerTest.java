package com.ruoyi.wash.order.controller;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.statemachine.OrderEvent;
import com.ruoyi.wash.network.service.WashCabinetQueryService;
import com.ruoyi.wash.network.service.WashSlotService;
import com.ruoyi.wash.order.service.WashOrderPaySupportService;
import com.ruoyi.wash.order.state.WashOrderStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 接入层测试：柜机回调「存入钥匙」接口（WxOrderSlotController.deposit）。
 *
 * <p>纯 Mockito 单测（与 OrderStateMachineTest 同一哲学：脱离 Spring，只验接入层规则），
 * 覆盖：开箱码正确 / 缺参 / 开箱码无效 / 开箱码失效 四条路径。
 */
@DisplayName("柜机回调：存入钥匙")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WxOrderSlotControllerTest {

    private static final String CODE_KEY = "slot:code:ORDER1";

    @Mock
    private WashSlotService slotService;
    @Mock
    private WashCabinetQueryService cabinetQueryService;
    @Mock
    private WashOrderPaySupportService orderSupport;
    @Mock
    private WashOrderStateService stateService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private WxOrderSlotController controller;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("开箱码正确 → 占用格口 + 触发 DEPOSIT_KEY + 删除码")
    void depositSuccess() {
        when(valueOps.get(CODE_KEY)).thenReturn("123456");

        var result = controller.deposit(new WxOrderSlotController.DepositRequest("ORDER1", "123456", "A02"));

        assertEquals(0, result.getCode());
        assertEquals("ORDER1", result.getData().get("orderNo"));
        verify(slotService).occupy("ORDER1");
        verify(stateService).systemFire(eq("ORDER1"), eq(OrderEvent.DEPOSIT_KEY), anyString());
        verify(redisTemplate).delete(CODE_KEY);
    }

    @Test
    @DisplayName("缺少 orderNo / code → A0001，不占用格口")
    void depositMissingParam() {
        ApiException e = assertThrows(ApiException.class,
                () -> controller.deposit(new WxOrderSlotController.DepositRequest(null, "123456", "A02")));

        assertEquals(ErrorCode.A0001, e.getErrorCode());
        verify(slotService, never()).occupy(anyString());
    }

    @Test
    @DisplayName("开箱码不匹配 → B2004，不占用格口")
    void depositWrongCode() {
        when(valueOps.get(CODE_KEY)).thenReturn("999999");

        ApiException e = assertThrows(ApiException.class,
                () -> controller.deposit(new WxOrderSlotController.DepositRequest("ORDER1", "123456", "A02")));

        assertEquals(ErrorCode.B2004, e.getErrorCode());
        verify(slotService, never()).occupy(anyString());
    }

    @Test
    @DisplayName("开箱码已失效（null）→ B2004")
    void depositCodeExpired() {
        when(valueOps.get(CODE_KEY)).thenReturn(null);

        ApiException e = assertThrows(ApiException.class,
                () -> controller.deposit(new WxOrderSlotController.DepositRequest("ORDER1", "123456", "A02")));

        assertEquals(ErrorCode.B2004, e.getErrorCode());
    }
}
