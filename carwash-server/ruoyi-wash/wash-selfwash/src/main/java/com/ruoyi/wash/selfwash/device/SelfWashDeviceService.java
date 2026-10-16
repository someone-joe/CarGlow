package com.ruoyi.wash.selfwash.device;

/**
 * 自助洗车设备控制（通电压 / 断电）。
 *
 * <p>软件闭环期由 {@link MockSelfWashDeviceService} 打点模拟；硬件就绪后新增真实实现并切换即可，
 * 业务代码只依赖本接口，不感知具体渠道。
 */
public interface SelfWashDeviceService {

    /** 开始服务：设备通电（启动高压水枪等） */
    void powerOn(Long workstationId, String orderNo);

    /** 结束服务：设备断电 */
    void powerOff(Long workstationId, String orderNo);
}
