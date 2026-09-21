package com.ruoyi.wash.worker.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.statemachine.OrderEvent;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.service.WashMediaService;
import com.ruoyi.wash.order.service.WashOrderPickService;
import com.ruoyi.wash.order.state.WashOrderStateService;
import com.ruoyi.wash.worker.dto.PickTaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 师傅端取送业务：任务池、取车拍照完成、送回停放完成。
 *
 * <p>状态一律走 WashOrderStateService.workerFire（统一状态机），禁止直接 update 订单状态。
 */
@Service
public class WashPickService {

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

    @Autowired
    private WashWorkerService workerService;

    @Autowired
    private WashOrderPickService pickQuery;

    @Autowired
    private WashOrderStateService stateService;

    @Autowired
    private WashMediaService mediaService;

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
     * 取钥匙：师傅从柜中取出客户钥匙，开始取车作业（KEY_IN → PICKING）。
     * 物理开箱由柜机回调负责，这里只做作业动作的打卡与状态推进。
     */
    public void takeKey(String orderNo, Long workerId) {
        stateService.workerFire(orderNo, OrderEvent.TAKE_KEY, workerId, null);
    }

    /**
     * 取车拍照完成。契约要求 ≥6 张（含仪表盘），不足直接拒绝：
     * 少了事后无法界定车损责任，这是取送环节最容易扯皮的地方。
     */
    public void pickCarDone(String orderNo, List<String> fileIds, String note, Long workerId) {
        if (fileIds == null || fileIds.size() < photoMin) {
            throw new ApiException(ErrorCode.B4002);
        }
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
        stateService.workerFire(orderNo, OrderEvent.RETURN_DONE, workerId, parkingNo);
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
