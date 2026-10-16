package com.ruoyi.wash.selfwash.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 设备控制 mock 实现：只打点，不接真实硬件。
 *
 * <p>wash.selfwash.device-mock-enabled 默认 true；接真实设备时新增实现类并置 false（或按条件装配二选一）。
 */
@Service
public class MockSelfWashDeviceService implements SelfWashDeviceService {

    private static final Logger log = LoggerFactory.getLogger(MockSelfWashDeviceService.class);

    @Value("${wash.selfwash.device-mock-enabled:true}")
    private boolean mockEnabled;

    @Override
    public void powerOn(Long workstationId, String orderNo) {
        if (mockEnabled) {
            log.info("[设备mock] 通电 workstation={} order={}", workstationId, orderNo);
        }
    }

    @Override
    public void powerOff(Long workstationId, String orderNo) {
        if (mockEnabled) {
            log.info("[设备mock] 断电 workstation={} order={}", workstationId, orderNo);
        }
    }
}
