package com.ruoyi.wash.worker.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.statemachine.OrderEvent;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.service.WashMediaService;
import com.ruoyi.wash.order.service.WashOrderPickService;
import com.ruoyi.wash.network.domain.WashCabinet;
import com.ruoyi.wash.network.domain.WashSlot;
import com.ruoyi.wash.network.service.WashCabinetQueryService;
import com.ruoyi.wash.network.service.WashSlotCodeService;
import com.ruoyi.wash.network.service.WashSlotService;
import com.ruoyi.wash.order.dto.OpenCodeVO;
import com.ruoyi.wash.notify.domain.NotifyTemplate;
import com.ruoyi.wash.notify.service.NotifyService;
import com.ruoyi.wash.order.service.WashOrderPaySupportService;
import com.ruoyi.wash.order.state.WashOrderStateService;
import com.ruoyi.wash.worker.dto.PickTaskVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 师傅端取送业务：任务池、取车拍照完成、送回停放完成。
 *
 * <p>状态一律走 WashOrderStateService.workerFire（统一状态机），禁止直接 update 订单状态。
 */
@Service
public class WashPickService {

    private static final Logger log = LoggerFactory.getLogger(WashPickService.class);

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 待取车：钥匙已入柜待取 / 已取钥匙取车中 / 去程运输中。
     * 必须从 KEY_IN 起：师傅先从柜中取走钥匙（take-key）才有后续动作，
     * 少了 KEY_IN，师傅在任务池里看不到单，取钥匙接口就没有入口。
     */
    private static final List<String> PICKUP_STATUSES =
            List.of(OrderStatus.KEY_IN.name(), OrderStatus.PICKING.name(), OrderStatus.TO_STATION.name());

    /** 待送回：待还车 / 回程运输中 */
    private static final List<String> RETURN_STATUSES =
            List.of(OrderStatus.WAIT_RETURN.name(), OrderStatus.RETURNING.name());

    /** 取车必拍照片数（360° + 仪表盘），走配置不写死 */
    @Value("${wash.pick.photo-min:6}")
    private int photoMin;

    /** 用户停车照的业务类型：师傅找车看的就是这批照片 */
    private static final String BIZ_TYPE_PARK = "PARK";

    /** 取车照（360° + 仪表盘） */
    private static final String BIZ_TYPE_PICK = "PICK";

    /** 送回停放照（含车位号） */
    private static final String BIZ_TYPE_RETURN = "RETURN";

    @Autowired
    private WashWorkerService workerService;

    @Autowired
    private WashOrderPickService pickQuery;

    @Autowired
    private WashOrderStateService stateService;

    @Autowired
    private WashMediaService mediaService;

    /** 师傅取走钥匙后释放格口用（经 wash-order 传递依赖 wash-network，不新增直接依赖） */
    @Autowired
    private WashSlotService slotService;

    @Autowired
    private WashSlotCodeService codeService;

    @Autowired
    private WashCabinetQueryService cabinetQueryService;

    /** 按订单号查订单（无登录态查询入口，取钥匙前校验状态用） */
    @Autowired
    private WashOrderPaySupportService orderQuery;

    /** 消息通知（best-effort：推送失败只记日志，不阻断订单流转） */
    @Autowired
    private NotifyService notifyService;

    /** 开箱码有效期（分钟）：仅用于取码后算不出 TTL 时的兜底，避免倒计时直接归零 */
    @Value("${wash.slot.open-code-ttl-minutes:10}")
    private long openCodeTtlMinutes;

    /**
     * 任务池。站点是硬过滤（师傅只看本站点单），stage 决定看待取还是待送。
     */
    public List<PickTaskVO> tasks(Long workerId, String stage) {
        Long siteId = workerService.requireSiteId(workerId);
        List<WashOrder> orders = pickQuery.selectBySiteAndStatuses(siteId, resolveStatuses(stage));

        List<PickTaskVO> list = new ArrayList<>(orders.size());
        for (WashOrder order : orders) {
            PickTaskVO vo = new PickTaskVO();
            vo.setOrderNo(order.getOrderNo());
            vo.setStatus(order.getStatus());
            // 中文状态名由后端枚举给出，前端只展示不翻译，避免两处漂移
            vo.setStatusLabel(OrderStatus.of(order.getStatus()).getLabel());
            // 用户停车照：师傅找车用（PARK，下单/存钥匙时上传）
            vo.setParkPhotos(mediaService.listVosByOrder(order.getOrderNo(), BIZ_TYPE_PARK));
            vo.setServiceName(order.getServiceName());
            vo.setVehiclePlate(order.getPlateNo());
            vo.setBizType(PICKUP_STATUSES.contains(order.getStatus()) ? "PICKUP" : "RETURN");
            vo.setAppointmentDate(formatDate(order.getAppointTime()));
            vo.setRequiredPhotoCount(photoMin);
            // 车辆品牌/颜色、客户信息、取送地址、承诺还车时间：等待对应域接入后填充
            list.add(vo);
        }
        return list;
    }

    /**
     * 取钥匙的开箱码：扫码为主、码兜底。仅「钥匙已入柜 KEY_IN」可开箱。
     *
     * <p>传了 slotNo（扫柜机格口二维码所得）就做格口匹配校验：一物一码，
     * 扫错柜比取不到钥匙更糟——开的是别人的格口。不传则只取备用码（扫码失败、柜机离线时的兜底）。
     *
     * <p>MVP 无柜机，扫码只做到「格口匹配」为止；接入硬件后开箱应改由柜机回执驱动（见 PROGRESS 挂起项）。
     */
    public OpenCodeVO openCode(String orderNo, String slotNo, Long workerId) {
        WashOrder order = orderQuery.require(orderNo);
        if (!OrderStatus.KEY_IN.name().equals(order.getStatus())) {
            throw new ApiException(ErrorCode.B1002, "当前状态无需开箱，状态：" + order.getStatus());
        }
        WashSlot slot = slotService.require(orderNo);
        if (slotNo != null && !slotNo.isBlank() && !slotNo.equals(slot.getSlotNo())) {
            throw new ApiException(ErrorCode.B2005, "扫码格口与本单格口不符，本单格口：" + slot.getSlotNo());
        }
        WashCabinet cabinet = cabinetQueryService.requireEnabled(order.getCabinetId());

        String code = codeService.issue(WashSlotCodeService.WORKER_PREFIX, orderNo);
        // 取码即留痕：开箱凭证的发放本身就是可审计动作（PRD 6.2 留痕完整率 100%）
        slotService.logOpen(slot, "OPEN_CODE", code, WashSlotService.ROLE_WORKER, workerId,
                WashSlotService.SOURCE_WORKER, "SUCCESS");

        OpenCodeVO vo = new OpenCodeVO();
        vo.setCode(code);
        vo.setCabinetName(cabinet.getCabinetName());
        vo.setSlotNo(slot.getSlotNo());
        // 重复取码不重置时效：取剩余 TTL（与 C 端 open-code 同一口径）
        long remainMs = codeService.expireMillis(WashSlotCodeService.WORKER_PREFIX, orderNo);
        vo.setExpireAt(System.currentTimeMillis() + (remainMs > 0 ? remainMs : openCodeTtlMinutes * 60_000));
        return vo;
    }

    /**
     * 取钥匙：师傅从柜中取出客户钥匙，开始取车作业（KEY_IN → PICKING）。
     *
     * <p>必带开箱码且先校验再推进状态：顺序反了会出现「状态已走但箱没开」，
     * 留痕也就成了事后补录（PRD 6.4 要求开箱动作真实发生）。
     * 正式环境应由柜机开箱回执驱动，MVP 无柜机，由师傅在开箱页确认后提交。
     */
    public void takeKey(String orderNo, String code, Long workerId) {
        WashSlot slot = slotService.require(orderNo);
        codeService.verify(WashSlotCodeService.WORKER_PREFIX, orderNo, code);
        slotService.logOpen(slot, "OPEN", code, WashSlotService.ROLE_WORKER, workerId,
                WashSlotService.SOURCE_WORKER, "SUCCESS");
        codeService.clear(WashSlotCodeService.WORKER_PREFIX, orderNo);
        stateService.workerFire(orderNo, OrderEvent.TAKE_KEY, workerId, null);
        // 钥匙已被师傅取走，格口必须腾出，否则该格永远占用（bug092410）
        slotService.release(orderNo, WashSlotService.ROLE_WORKER, workerId, WashSlotService.SOURCE_WORKER);
    }

    /**
     * 取车拍照完成。契约要求 ≥6 张（含仪表盘），不足直接拒绝：
     * 少了事后无法界定车损责任，这是取送环节最容易扯皮的地方。
     */
    public void pickCarDone(String orderNo, List<String> fileIds, String note, Long workerId) {
        if (fileIds == null || fileIds.size() < photoMin) {
            throw new ApiException(ErrorCode.B4002);
        }
        // 光校验数量不够：传任意字符串都能推进状态，事后无法证明照片是本单的车
        mediaService.requireBelongToOrder(fileIds, orderNo, BIZ_TYPE_PICK);
        stateService.workerFire(orderNo, OrderEvent.PICK_CAR_DONE, workerId, note);
    }

    /** 送回停放 + 钥匙归柜。车位号必填，用于事后定位车辆。 */
    public void returnDone(String orderNo, List<String> fileIds, String parkingNo, Long workerId) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new ApiException(ErrorCode.B4002);
        }
        if (parkingNo == null || parkingNo.isBlank()) {
            throw new ApiException(ErrorCode.A0001, "车位号必填");
        }
        mediaService.requireBelongToOrder(fileIds, orderNo, BIZ_TYPE_RETURN);
        var order = orderQuery.require(orderNo);
        stateService.workerFire(orderNo, OrderEvent.RETURN_DONE, workerId, parkingNo);
        // 已归位待取钥匙 → 推「车辆已洗好，请到钥匙柜取钥匙」（需求 11.2）
        try {
            Map<String, String> data = new HashMap<>();
            data.put("thing1", "车辆已洗好");
            data.put("thing2", parkingNo);
            notifyService.send(order.getMemberId(), NotifyTemplate.KEY_READY, data, orderNo + ":KEY_READY");
        } catch (Exception e) {
            log.warn("[送回完成] 推送取钥匙通知失败 order={}", orderNo, e);
        }
    }

    private List<String> resolveStatuses(String stage) {
        if ("PICKUP".equals(stage)) {
            return PICKUP_STATUSES;
        }
        if ("RETURN".equals(stage)) {
            return RETURN_STATUSES;
        }
        List<String> all = new ArrayList<>(PICKUP_STATUSES);
        all.addAll(RETURN_STATUSES);
        return all;
    }

    private String formatDate(Long millis) {
        return millis == null ? null : Instant.ofEpochMilli(millis).atZone(ZONE).toLocalDate().format(DATE_FMT);
    }
}
