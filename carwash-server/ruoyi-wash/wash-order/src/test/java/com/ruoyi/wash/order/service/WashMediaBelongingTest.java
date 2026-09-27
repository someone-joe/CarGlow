package com.ruoyi.wash.order.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.order.domain.WashMedia;
import com.ruoyi.wash.order.mapper.WashMediaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.junit.jupiter.api.function.Executable;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 影像归属校验（E-2）：此前各入口只校验照片数量，传任意字符串都能推进状态。
 *
 * <p>这里覆盖「必须拦住」的四类情况：不存在、跨单、错环节、以及下单前停车照被人冒用。
 */
@DisplayName("影像归属校验：WashMediaService")
@ExtendWith(MockitoExtension.class)
class WashMediaBelongingTest {

    @Mock private WashMediaMapper mediaMapper;

    private WashMediaService service;

    private static final String ORDER = "SE20260927120000000001";
    private static final String OTHER_ORDER = "SE20260927120000000002";

    @BeforeEach
    void init() {
        service = new WashMediaService();
        inject(service, "mediaMapper", mediaMapper);
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

    private WashMedia media(String fileId, String orderNo, String bizType, Long memberId) {
        WashMedia m = new WashMedia();
        m.setFileId(fileId);
        m.setOrderNo(orderNo);
        m.setBizType(bizType);
        m.setMemberId(memberId);
        return m;
    }

    private ApiException assertB1005(Executable runnable) {
        ApiException ex = assertThrows(ApiException.class, runnable);
        assertEquals(ErrorCode.B1005, ex.getErrorCode(), "应返回影像归属错误码 B1005");
        return ex;
    }

    @Test
    @DisplayName("属于本单且环节匹配：放行")
    void belong_sameOrderAndType_pass() {
        when(mediaMapper.selectByFileId("f1")).thenReturn(media("f1", ORDER, "PICK", 1L));
        assertDoesNotThrow(() -> service.requireBelongToOrder(List.of("f1"), ORDER, "PICK"));
    }

    @Test
    @DisplayName("空列表：不报错（数量由调用方各自的必拍张数约束）")
    void belong_emptyList_noop() {
        assertDoesNotThrow(() -> service.requireBelongToOrder(List.of(), ORDER, "PICK"));
        assertDoesNotThrow(() -> service.requireBelongToOrder(null, ORDER, "PICK"));
        verifyNoInteractions(mediaMapper);
    }

    @Test
    @DisplayName("fileId 不存在：拒绝（此前传任意字符串都能过）")
    void belong_missingFileId_rejected() {
        when(mediaMapper.selectByFileId("bogus")).thenReturn(null);
        assertB1005(() -> service.requireBelongToOrder(List.of("bogus"), ORDER, "PICK"));
    }

    @Test
    @DisplayName("属于别的订单：拒绝，防止跨单复用照片")
    void belong_otherOrder_rejected() {
        when(mediaMapper.selectByFileId("f1")).thenReturn(media("f1", OTHER_ORDER, "PICK", 1L));
        assertB1005(() -> service.requireBelongToOrder(List.of("f1"), ORDER, "PICK"));
    }

    @Test
    @DisplayName("bizType 不匹配：拒绝，防止拿别环节的照片顶替")
    void belong_wrongBizType_rejected() {
        when(mediaMapper.selectByFileId("f1")).thenReturn(media("f1", ORDER, "WASHED", 1L));
        assertB1005(() -> service.requireBelongToOrder(List.of("f1"), ORDER, "PICK"));
    }

    @Test
    @DisplayName("取车照多张：只要有一张不符就整批拒绝")
    void belong_oneBadOfMany_rejected() {
        when(mediaMapper.selectByFileId("f1")).thenReturn(media("f1", ORDER, "PICK", 1L));
        when(mediaMapper.selectByFileId("f2")).thenReturn(media("f2", OTHER_ORDER, "PICK", 1L));
        assertB1005(() -> service.requireBelongToOrder(List.of("f1", "f2"), ORDER, "PICK"));
    }

    @Test
    @DisplayName("下单停车照：本人的 PARK 且尚未归属 → 放行")
    void parkPhoto_ownAndUnbound_pass() {
        when(mediaMapper.selectByFileId("p1")).thenReturn(media("p1", null, "PARK", 1L));
        assertDoesNotThrow(() -> service.requireOwnParkPhotos(List.of("p1"), 1L));
    }

    @Test
    @DisplayName("下单停车照：别人的照片 → 拒绝")
    void parkPhoto_otherMember_rejected() {
        when(mediaMapper.selectByFileId("p1")).thenReturn(media("p1", null, "PARK", 2L));
        assertB1005(() -> service.requireOwnParkPhotos(List.of("p1"), 1L));
    }

    @Test
    @DisplayName("下单停车照：已被别的单领走 → 拒绝（防止复用旧照片）")
    void parkPhoto_alreadyBound_rejected() {
        when(mediaMapper.selectByFileId("p1")).thenReturn(media("p1", OTHER_ORDER, "PARK", 1L));
        assertB1005(() -> service.requireOwnParkPhotos(List.of("p1"), 1L));
    }

    @Test
    @DisplayName("下单停车照：非 PARK 类型 → 拒绝")
    void parkPhoto_wrongType_rejected() {
        when(mediaMapper.selectByFileId("p1")).thenReturn(media("p1", null, "PICK", 1L));
        assertB1005(() -> service.requireOwnParkPhotos(List.of("p1"), 1L));
    }

    @Test
    @DisplayName("回填订单号：交给 mapper，且空列表不触发写库")
    void bindToOrder_delegatesToMapper() {
        service.bindToOrder(List.of("p1"), ORDER);
        verify(mediaMapper).bindOrderNo(List.of("p1"), ORDER);

        service.bindToOrder(List.of(), ORDER);
        service.bindToOrder(null, ORDER);
        verifyNoMoreInteractions(mediaMapper);
    }
}
