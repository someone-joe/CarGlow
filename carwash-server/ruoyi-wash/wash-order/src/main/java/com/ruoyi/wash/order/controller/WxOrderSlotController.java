package com.ruoyi.wash.order.controller;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.common.statemachine.OrderEvent;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.network.domain.WashCabinet;
import com.ruoyi.wash.network.domain.WashSlot;
import com.ruoyi.wash.network.service.WashCabinetQueryService;
import com.ruoyi.wash.network.service.WashSlotService;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.dto.OpenCodeVO;
import com.ruoyi.wash.order.service.WashOrderPaySupportService;
import com.ruoyi.wash.order.state.WashOrderStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;

/**
 * 钥匙存取：开箱码 + 柜机回调。
 *
 * <p>契约：openapi.yaml /api/v1/orders/{orderNo}/open-code、/device-callback/v1/slot/deposit。
 */
@RestController
public class WxOrderSlotController {

    private static final String CODE_PREFIX = "slot:code:";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private WashSlotService slotService;

    @Autowired
    private WashCabinetQueryService cabinetQueryService;

    @Autowired
    private WashOrderPaySupportService orderSupport;

    @Autowired
    private WashOrderStateService stateService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 取开箱码：仅待存钥匙 / 待取钥匙（取回）两个环节可用 */
    @PostMapping("/api/v1/orders/{orderNo}/open-code")
    public ApiResult<OpenCodeVO> openCode(@PathVariable String orderNo) {
        Long memberId = MemberContext.require();
        WashOrder order = orderSupport.requireOwned(orderNo, memberId);
        if (!OrderStatus.WAIT_KEY.name().equals(order.getStatus())
                && !OrderStatus.RETURNED.name().equals(order.getStatus())) {
            throw new ApiException(ErrorCode.B1002, "当前状态无需开箱，状态：" + order.getStatus());
        }

        WashSlot slot = slotService.require(orderNo);
        WashCabinet cabinet = cabinetQueryService.requireEnabled(order.getCabinetId());

        String code = String.valueOf(100000 + RANDOM.nextInt(900000));
        stringRedisTemplate.opsForValue().set(CODE_PREFIX + orderNo, code, Duration.ofMinutes(10));
        slotService.logOpenCode(slot, code);

        OpenCodeVO vo = new OpenCodeVO();
        vo.setCode(code);
        vo.setCabinetName(cabinet.getCabinetName());
        vo.setSlotNo(slot.getSlotNo());
        // 前端用 expireAt 做倒计时；取 Redis 剩余 TTL，重复取码时不会重置成完整 10 分钟（bug092406）
        long ttlSec = stringRedisTemplate.getExpire(CODE_PREFIX + orderNo);
        long remainMs = ttlSec > 0 ? ttlSec * 1000 : Duration.ofMinutes(10).toMillis();
        vo.setExpireAt(System.currentTimeMillis() + remainMs);
        return ApiResult.ok(vo);
    }

    /**
     * 柜机回调：钥匙已存入 → 格口占用 + 状态机 DEPOSIT_KEY（WAIT_KEY → KEY_IN）。
     *
     * <p>TODO（接入硬件时必补）：柜机签名校验 + IP 白名单 + 回调幂等（同一 orderNo 只处理一次）。
     * 当前仅校验开箱码，状态机本身会拒绝重复流转，因此不会重复推进。
     */
    @PostMapping("/device-callback/v1/slot/deposit")
    public ApiResult<Map<String, String>> deposit(@RequestBody DepositRequest request) {
        if (request == null || request.orderNo() == null || request.code() == null) {
            throw new ApiException(ErrorCode.A0001, "缺少 orderNo / code");
        }
        String expected = stringRedisTemplate.opsForValue().get(CODE_PREFIX + request.orderNo());
        if (expected == null || !expected.equals(request.code())) {
            throw new ApiException(ErrorCode.B2004);
        }
        slotService.occupy(request.orderNo());
        stateService.systemFire(request.orderNo(), OrderEvent.DEPOSIT_KEY, "柜机回调：钥匙已存入");
        stringRedisTemplate.delete(CODE_PREFIX + request.orderNo());
        return ApiResult.ok(Map.of("orderNo", request.orderNo()));
    }

    public record DepositRequest(String orderNo, String code, String slotNo) {
    }
}
