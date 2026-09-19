package com.ruoyi.wash.order.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.goods.domain.WashService;
import com.ruoyi.wash.goods.service.WashServiceQueryService;
import com.ruoyi.wash.member.domain.WashVehicle;
import com.ruoyi.wash.member.service.WashVehicleQueryService;
import com.ruoyi.wash.network.domain.WashCabinet;
import com.ruoyi.wash.network.service.WashCabinetQueryService;
import com.ruoyi.wash.network.service.WashSlotService;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.dto.CreateOrderRequest;
import com.ruoyi.wash.order.dto.CreateOrderVO;
import com.ruoyi.wash.order.mapper.WashOrderMapper;
import com.ruoyi.wash.promo.service.WashCouponService;
import com.ruoyi.common.core.redis.RedisCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@DisplayName("下单用券：WashOrderCreateService 接入优惠券")
@ExtendWith(MockitoExtension.class)
class WashOrderCreateCouponTest {

    @Mock private WashOrderMapper orderMapper;
    @Mock private WashServiceQueryService serviceQueryService;
    @Mock private WashVehicleQueryService vehicleQueryService;
    @Mock private WashCabinetQueryService cabinetQueryService;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private WashSlotService slotService;
    @Mock private WashCouponService couponService;
    @Mock private RedisCache redisCache;

    private WashOrderCreateService service;

    private static final long MEMBER = 123L;
    private static final long PRICE = 3900L;

    @BeforeEach
    void init() {
        service = new WashOrderCreateService();
        inject(service, "orderMapper", orderMapper);
        inject(service, "serviceQueryService", serviceQueryService);
        inject(service, "vehicleQueryService", vehicleQueryService);
        inject(service, "cabinetQueryService", cabinetQueryService);
        inject(service, "stringRedisTemplate", stringRedisTemplate);
        inject(service, "slotService", slotService);
        inject(service, "couponService", couponService);
        inject(service, "redisCache", redisCache);
        // 幂等键首次提交，无已存在订单
        when(redisCache.getCacheObject(anyString())).thenReturn(null);
        // 产能脚本返回 -1：当日未设置产能，不拦截
        when(stringRedisTemplate.execute(any(), anyList())).thenReturn(-1L);
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

    private CreateOrderRequest baseRequest(Long couponUserId) {
        return new CreateOrderRequest(1L, 1L, 1L, "2026-09-20", "19:00",
                true, null, null, true, couponUserId);
    }

    private void stubQuery() {
        WashService s = new WashService();
        s.setServiceName("标准洗车");
        s.setPriceAmount(PRICE);
        WashVehicle v = new WashVehicle();
        v.setVehicleId(1L);
        v.setPlateNo("京A12345");
        WashCabinet c = new WashCabinet();
        c.setSiteId(10L);
        c.setCommunityId(20L);
        c.setCabinetId(1L);
        when(serviceQueryService.requireEnabled(1L)).thenReturn(s);
        when(vehicleQueryService.requireOwned(MEMBER, 1L)).thenReturn(v);
        when(cabinetQueryService.requireEnabled(1L)).thenReturn(c);
        lenient().when(orderMapper.insertWashOrder(any(WashOrder.class))).thenReturn(1);
    }

    @Test
    @DisplayName("不用券：payAmount 等于服务价，couponDiscount=0")
    void create_withoutCoupon_payAmountUntouched() {
        stubQuery();
        CreateOrderVO vo = service.create(MEMBER, baseRequest(null), "idem-1");
        assertEquals(PRICE, vo.getPayAmount());
        assertEquals(0L, vo.getCouponDiscount());
        verify(couponService, never()).applyToOrder(anyLong(), anyLong(), anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("用券：payAmount 扣减，couponDiscount 返回抵扣额")
    void create_withCoupon_discountApplied() {
        stubQuery();
        when(couponService.applyToOrder(eq(MEMBER), eq(11L), eq(PRICE), anyString(), anyLong())).thenReturn(500L);

        CreateOrderVO vo = service.create(MEMBER, baseRequest(11L), "idem-2");

        assertEquals(PRICE - 500, vo.getPayAmount());
        assertEquals(500L, vo.getCouponDiscount());
        verify(couponService).applyToOrder(eq(MEMBER), eq(11L), eq(PRICE), anyString(), anyLong());
    }

    @Test
    @DisplayName("用券被拒（券不可用）：原样抛出 C1003，不下单")
    void create_withInvalidCoupon_throwsAndDoesNotCreate() {
        stubQuery();
        when(couponService.applyToOrder(eq(MEMBER), eq(11L), eq(PRICE), anyString(), anyLong()))
                .thenThrow(new ApiException(com.ruoyi.wash.common.api.ErrorCode.C1003, "优惠券不可用"));

        assertThrows(ApiException.class, () -> service.create(MEMBER, baseRequest(11L), "idem-3"));
        verify(orderMapper, never()).insertWashOrder(any(WashOrder.class));
    }
}
