package com.ruoyi.wash.worker.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.statemachine.OrderEvent;
import com.ruoyi.wash.network.domain.WashSlot;
import com.ruoyi.wash.network.service.WashCabinetQueryService;
import com.ruoyi.wash.network.service.WashSlotCodeService;
import com.ruoyi.wash.network.service.WashSlotService;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.service.WashMediaService;
import com.ruoyi.wash.order.service.WashOrderPaySupportService;
import com.ruoyi.wash.order.service.WashOrderPickService;
import com.ruoyi.wash.order.state.WashOrderStateService;
import com.ruoyi.wash.notify.service.NotifyService;
import com.ruoyi.wash.worker.dto.PickTaskVO;
import com.ruoyi.wash.worker.service.WashDispatchService;
import com.ruoyi.wash.worker.service.WashPickService;
import com.ruoyi.wash.worker.service.WashWorkerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 取送业务：重点覆盖"送回归还"环节的格口重占（修复 bug：钥匙随车送回后必须重新入柜，
 * 否则客户取钥匙时查不到本单格口会误报"钥匙柜离线" B2001）。
 */
@DisplayName("取送业务：WashPickService")
@ExtendWith(MockitoExtension.class)
class WashPickServiceTest {

    @Mock private WashWorkerService workerService;
    @Mock private WashDispatchService dispatchService;
    @Mock private WashOrderPickService pickQuery;
    @Mock private WashOrderStateService stateService;
    @Mock private WashMediaService mediaService;
    @Mock private WashSlotService slotService;
    @Mock private WashSlotCodeService codeService;
    @Mock private WashCabinetQueryService cabinetQueryService;
    @Mock private WashOrderPaySupportService orderQuery;
    @Mock private NotifyService notifyService;

    private WashPickService service;

    private static final String ORDER_NO = "SE20261017212906841592";
    private static final Long WORKER_ID = 1L;
    private static final Long MEMBER_ID = 1L;
    private static final Long CABINET_ID = 304L;

    @BeforeEach
    void init() {
        service = new WashPickService();
        inject(service, "workerService", workerService);
        inject(service, "dispatchService", dispatchService);
        inject(service, "pickQuery", pickQuery);
        inject(service, "stateService", stateService);
        inject(service, "mediaService", mediaService);
        inject(service, "slotService", slotService);
        inject(service, "codeService", codeService);
        inject(service, "cabinetQueryService", cabinetQueryService);
        inject(service, "orderQuery", orderQuery);
        inject(service, "notifyService", notifyService);
    }

    private void inject(Object target, String field, Object value) {
        try {
            var f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private WashOrder returnedOrder() {
        WashOrder order = new WashOrder();
        order.setOrderNo(ORDER_NO);
        order.setCabinetId(CABINET_ID);
        order.setMemberId(MEMBER_ID);
        order.setStatus("RETURNED");
        return order;
    }

    @Test
    @DisplayName("送回归还：本单无占用格口时，重新预占并占用同一机柜的格口")
    void returnDone_noSlot_reoccupies() {
        when(orderQuery.require(ORDER_NO)).thenReturn(returnedOrder());
        // 模拟 takeKey 已释放：require 抛 B2001（查不到本单格口）
        when(slotService.require(ORDER_NO)).thenThrow(new ApiException(ErrorCode.B2001));

        service.returnDone(ORDER_NO, List.of("f1"), "A01", WORKER_ID);

        // 状态机推进到 RETURNED
        verify(stateService).workerFire(ORDER_NO, OrderEvent.RETURN_DONE, WORKER_ID, "A01");
        // 重新入柜：必须用订单所属机柜，且 reserve + occupy 都被调用
        verify(slotService).reserve(CABINET_ID, ORDER_NO, MEMBER_ID);
        verify(slotService).occupy(ORDER_NO);
    }

    @Test
    @DisplayName("送回归还：本单已占用格口（重复送回/补偿）时，幂等跳过，不重复绑定格口")
    void returnDone_hasSlot_idempotentSkip() {
        when(orderQuery.require(ORDER_NO)).thenReturn(returnedOrder());
        WashSlot existing = new WashSlot();
        existing.setSlotId(406L);
        existing.setSlotNo("111");
        // 已有占用格口：require 不抛异常
        when(slotService.require(ORDER_NO)).thenReturn(existing);

        service.returnDone(ORDER_NO, List.of("f1"), "A01", WORKER_ID);

        verify(stateService).workerFire(ORDER_NO, OrderEvent.RETURN_DONE, WORKER_ID, "A01");
        verify(slotService, never()).reserve(anyLong(), anyString(), anyLong());
        verify(slotService, never()).occupy(anyString());
    }

    @Test
    @DisplayName("送回归还：车位号为空 → 拒绝，且不会触碰格口")
    void returnDone_missingParkingNo_rejected() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.returnDone(ORDER_NO, List.of("f1"), "  ", WORKER_ID));
        assertEquals(ErrorCode.A0001, ex.getErrorCode());
        verify(slotService, never()).reserve(anyLong(), anyString(), anyLong());
        verify(slotService, never()).occupy(anyString());
    }

    @Test
    @DisplayName("送回归还：无送回停放照 → 拒绝，且不会触碰格口")
    void returnDone_noPhotos_rejected() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.returnDone(ORDER_NO, List.of(), "A01", WORKER_ID));
        assertEquals(ErrorCode.B4002, ex.getErrorCode());
        verify(slotService, never()).reserve(anyLong(), anyString(), anyLong());
        verify(slotService, never()).occupy(anyString());
    }
}
