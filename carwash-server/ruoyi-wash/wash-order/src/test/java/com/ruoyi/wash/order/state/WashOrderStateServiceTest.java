package com.ruoyi.wash.order.state;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.statemachine.OrderEvent;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.mapper.WashOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 接入层测试：后台推进服务（WashOrderStateService.adminFire）的规则与状态机接入。
 *
 * <p>纯 Mockito 单测：用内存 Map 模拟「数据库状态」，把状态机闸机真实接入，
 * 验证「后台干预收敛」的三重约束（白名单 / 原因必填 / 高危二次确认标记）与状态流转是否正确驱动。
 * 不启动 Spring，锁与状态读写均为 mock。
 */
@DisplayName("后台推进服务 adminFire")
class WashOrderStateServiceTest {

    private final OrderRedisLock lock = mock(OrderRedisLock.class);
    private final WashOrderStatusAccessor accessor = mock(WashOrderStatusAccessor.class);
    private final WashOrderMapper mapper = mock(WashOrderMapper.class);
    private final WashOrderStateService service = new WashOrderStateService(lock, accessor, mapper);

    /** 模拟「库里当前状态」：loadStatus 读它、updateStatus 写它。 */
    private final Map<String, OrderStatus> db = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        when(accessor.loadStatus(anyString())).thenAnswer(inv -> db.get(inv.getArgument(0)));
        when(accessor.updateStatus(anyString(), any(), any())).thenAnswer(inv -> {
            String no = inv.getArgument(0);
            OrderStatus from = inv.getArgument(1);
            OrderStatus to = inv.getArgument(2);
            if (db.get(no) == from) {
                db.put(no, to);
                return true;
            }
            return false;
        });

        // writeLog 副作用需要 orderMapper.selectByOrderNo / insertStatusLog
        // 注意 adminFire 会用 order.getOrderNo() 重建上下文，故返回的 order 的 orderNo 必须跟随入参
        when(mapper.selectByOrderNo(anyString())).thenAnswer(inv -> {
            WashOrder o = new WashOrder();
            o.setOrderNo(inv.getArgument(0));
            o.setStatus("WAIT_KEY");
            return o;
        });
        when(mapper.insertStatusLog(any())).thenReturn(1);

        // 手动触发 @PostConstruct，把状态机闸机接上（锁=lock，状态读写=accessor）
        service.init();
    }

    @Test
    @DisplayName("白名单：PAY_SUCCESS / APPLY_REFUND / REVIEW_SUBMIT 禁止后台触发，TAKE_KEY / DEPOSIT_KEY / QC_FAIL 允许")
    void adminAllowedRules() {
        assertFalse(WashOrderStateService.isAdminAllowed(OrderEvent.PAY_SUCCESS));
        assertFalse(WashOrderStateService.isAdminAllowed(OrderEvent.APPLY_REFUND));
        assertFalse(WashOrderStateService.isAdminAllowed(OrderEvent.REVIEW_SUBMIT));
        assertTrue(WashOrderStateService.isAdminAllowed(OrderEvent.TAKE_KEY));
        assertTrue(WashOrderStateService.isAdminAllowed(OrderEvent.DEPOSIT_KEY));
        assertTrue(WashOrderStateService.isAdminAllowed(OrderEvent.QC_FAIL));
    }

    @Test
    @DisplayName("二次确认标记：TAKE_KEY_BACK / QC_FAIL 需确认，TAKE_KEY 不需要")
    void confirmRequiredRules() {
        assertTrue(WashOrderStateService.isConfirmRequired(OrderEvent.TAKE_KEY_BACK));
        assertTrue(WashOrderStateService.isConfirmRequired(OrderEvent.QC_FAIL));
        assertFalse(WashOrderStateService.isConfirmRequired(OrderEvent.TAKE_KEY));
    }

    @Test
    @DisplayName("不在白名单的事件（PAY_SUCCESS）→ B1002")
    void adminFireDisallowedThrows() {
        ApiException e = assertThrows(ApiException.class,
                () -> service.adminFire("O", OrderEvent.PAY_SUCCESS, 1L, "x"));
        assertEquals(ErrorCode.B1002, e.getErrorCode());
    }

    @Test
    @DisplayName("原因空白 → A0001")
    void adminFireBlankReasonThrows() {
        ApiException e = assertThrows(ApiException.class,
                () -> service.adminFire("O", OrderEvent.TAKE_KEY, 1L, "  "));
        assertEquals(ErrorCode.A0001, e.getErrorCode());
    }

    @Test
    @DisplayName("订单不存在 → B1001")
    void adminFireOrderNotFoundThrows() {
        when(mapper.selectByOrderNo("MISSING")).thenReturn(null);
        ApiException e = assertThrows(ApiException.class,
                () -> service.adminFire("MISSING", OrderEvent.TAKE_KEY, 1L, "x"));
        assertEquals(ErrorCode.B1001, e.getErrorCode());
    }

    @Test
    @DisplayName("KEY_IN + TAKE_KEY → PICKING，且确实驱动了状态机写入")
    void adminFireTakeKeyAdvance() {
        db.put("O1", OrderStatus.KEY_IN);

        OrderStatus next = service.adminFire("O1", OrderEvent.TAKE_KEY, 1L, "取钥匙");

        assertEquals(OrderStatus.PICKING, next);
        assertEquals(OrderStatus.PICKING, db.get("O1"));
        verify(accessor).updateStatus("O1", OrderStatus.KEY_IN, OrderStatus.PICKING);
    }

    @Test
    @DisplayName("QC + QC_FAIL → WASHING（质检打回，状态机正确自环回清洗中）")
    void adminFireQcFailRollback() {
        db.put("O2", OrderStatus.QC);

        OrderStatus next = service.adminFire("O2", OrderEvent.QC_FAIL, 1L, "质检不通过");

        assertEquals(OrderStatus.WASHING, next);
        assertEquals(OrderStatus.WASHING, db.get("O2"));
    }

    @Test
    @DisplayName("RETURNED + TAKE_KEY_BACK → WAIT_REVIEW（客户取回钥匙，收尾环节）")
    void adminFireTakeKeyBack() {
        db.put("O3", OrderStatus.RETURNED);

        OrderStatus next = service.adminFire("O3", OrderEvent.TAKE_KEY_BACK, 1L, "客户取回钥匙");

        assertEquals(OrderStatus.WAIT_REVIEW, next);
    }

    @Test
    @DisplayName("WAIT_KEY + POSTPONE → WAIT_KEY（顺延自环，状态不变）")
    void adminFirePostponeSelfLoop() {
        db.put("O4", OrderStatus.WAIT_KEY);

        OrderStatus next = service.adminFire("O4", OrderEvent.POSTPONE, 1L, "顺延");

        assertEquals(OrderStatus.WAIT_KEY, next);
    }
}
