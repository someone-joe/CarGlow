package com.ruoyi.wash.selfwash.service;

import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.selfwash.dto.SelfwashPricingVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 自助洗车计时计价配置（需求：自助按计时收费，参数后台可配）。
 *
 * <p>存 sys_config，缺省值即产品预期：20 元 / 30 分钟，超时 8 元 / 10 分钟。
 * 读走 RuoYi 配置缓存，改后即时生效。
 */
@Service
public class WashSelfwashPricingService {

    private static final String K_BASE_AMOUNT = "wash.selfwash.base_amount";
    private static final String K_BASE_MINUTES = "wash.selfwash.base_minutes";
    private static final String K_OT_AMOUNT = "wash.selfwash.overtime_amount";
    private static final String K_OT_MINUTES = "wash.selfwash.overtime_minutes";

    private static final long DEF_BASE_AMOUNT = 2000L;
    private static final int DEF_BASE_MINUTES = 30;
    private static final long DEF_OT_AMOUNT = 800L;
    private static final int DEF_OT_MINUTES = 10;

    @Autowired
    private ISysConfigService sysConfigService;

    /** 当前生效的计价参数（配置缺失时用默认值） */
    public SelfwashPricingVO get() {
        return new SelfwashPricingVO(
                parseLong(K_BASE_AMOUNT, DEF_BASE_AMOUNT),
                parseInt(K_BASE_MINUTES, DEF_BASE_MINUTES),
                parseLong(K_OT_AMOUNT, DEF_OT_AMOUNT),
                parseInt(K_OT_MINUTES, DEF_OT_MINUTES));
    }

    /** 保存计价参数（后台）。金额/时长必须为正。 */
    @Transactional(rollbackFor = Exception.class)
    public SelfwashPricingVO save(long baseAmount, int baseMinutes, long overtimeAmount, int overtimeMinutes) {
        if (baseAmount <= 0 || baseMinutes <= 0) {
            throw new ApiException(ErrorCode.A0001, "基础金额与基础时长必须大于 0");
        }
        if (overtimeAmount < 0 || overtimeMinutes <= 0) {
            throw new ApiException(ErrorCode.A0001, "超时金额不能为负，超时时长必须大于 0");
        }
        upsert(K_BASE_AMOUNT, "自助洗车基础金额(分)", String.valueOf(baseAmount));
        upsert(K_BASE_MINUTES, "自助洗车基础时长(分钟)", String.valueOf(baseMinutes));
        upsert(K_OT_AMOUNT, "自助洗车超时金额(分)", String.valueOf(overtimeAmount));
        upsert(K_OT_MINUTES, "自助洗车超时时长(分钟)", String.valueOf(overtimeMinutes));
        return get();
    }

    /**
     * 加时费用（分）：按「overtimeAmount 分 / overtimeMinutes 分钟」整档计，不足一档按一档。
     */
    public long overtimeCost(int minutes) {
        if (minutes <= 0) {
            return 0;
        }
        SelfwashPricingVO p = get();
        long blocks = (minutes + p.overtimeMinutes() - 1) / p.overtimeMinutes();
        return blocks * p.overtimeAmount();
    }

    private void upsert(String key, String name, String value) {
        SysConfig query = new SysConfig();
        query.setConfigKey(key);
        List<SysConfig> list = sysConfigService.selectConfigList(query);
        if (list == null || list.isEmpty()) {
            SysConfig config = new SysConfig();
            config.setConfigName(name);
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setConfigType("N");
            sysConfigService.insertConfig(config);
            return;
        }
        SysConfig exist = list.get(0);
        exist.setConfigValue(value);
        sysConfigService.updateConfig(exist);
    }

    private long parseLong(String key, long def) {
        String v = sysConfigService.selectConfigByKey(key);
        try {
            return v == null || v.isBlank() ? def : Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private int parseInt(String key, int def) {
        String v = sysConfigService.selectConfigByKey(key);
        try {
            return v == null || v.isBlank() ? def : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
