package com.ruoyi.wash.member.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.wash.member.domain.WashCsFaq;
import com.ruoyi.wash.member.dto.CsConfigVO;
import com.ruoyi.wash.member.dto.CsFaq;
import com.ruoyi.wash.member.mapper.WashCsFaqMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 后台「客服配置」页读写。
 *
 * <p>标量字段（企微码、双电话、夜间提示）落 sys_config（与 C 端 WxConfigController 共用同一组 key，
 * 保证单一事实来源）；faqs（您可能会遇到的问题）落 wash_cs_faq 表（sys_config.config_value 仅 500 字，
 * 放不下 FAQ 列表，故独立建表）。前端 GET 取当前配置，POST 整体保存。
 *
 * <p>权限遵循 PRD 6.7.3 的 wash:*:* 命名；本页查看 wash:cs:query、保存 wash:cs:edit。
 */
@RestController
@RequestMapping("/admin-api/wash")
public class AdminCsConfigController {

    /** 平台电话最终兜底：未配置时与 C 端一致返回 10086，保证任何情况都拨得出去 */
    private static final String DEFAULT_PLATFORM_PHONE = "10086";

    private static final String KEY_QRCODE = "wash.cs.wecom-qrcode-url";
    private static final String KEY_PLATFORM_PHONE = "wash.cs.platform-phone";
    private static final String KEY_STATION_PHONE = "wash.cs.station-phone";
    private static final String KEY_NIGHT_TIP = "wash.cs.night-tip";

    @Autowired
    private ISysConfigService sysConfigService;

    @Autowired
    private WashCsFaqMapper faqMapper;

    @Value("${wash.customer-service.wecom-qrcode-url:}")
    private String wecomQrcodeUrl;

    @Value("${wash.customer-service.platform-phone:10086}")
    private String platformPhone;

    @Value("${wash.customer-service.station-phone:}")
    private String stationPhone;

    @Value("${wash.customer-service.night-tip:}")
    private String nightTip;

    @PreAuthorize("@ss.hasPermi('wash:cs:query')")
    @GetMapping("/cs-config")
    public AjaxResult get() {
        CsConfigVO vo = new CsConfigVO();
        vo.setWecomQrcodeUrl(resolve(KEY_QRCODE, wecomQrcodeUrl));
        String platform = resolve(KEY_PLATFORM_PHONE, platformPhone);
        vo.setPlatformPhone(platform == null || platform.isBlank() ? DEFAULT_PLATFORM_PHONE : platform);
        vo.setStationPhone(resolve(KEY_STATION_PHONE, stationPhone));
        vo.setNightTip(resolve(KEY_NIGHT_TIP, nightTip));
        vo.setFaqs(faqMapper.selectAll().stream()
                .map(f -> new CsFaq(f.getQuestion(), f.getAnswer()))
                .collect(Collectors.toList()));
        return AjaxResult.success(vo);
    }

    @PreAuthorize("@ss.hasPermi('wash:cs:edit')")
    @PostMapping("/cs-config")
    public AjaxResult save(@RequestBody CsConfigVO dto) {
        upsert(KEY_QRCODE, "客服-企微二维码URL", dto.getWecomQrcodeUrl());
        upsert(KEY_PLATFORM_PHONE, "客服-平台电话", dto.getPlatformPhone());
        upsert(KEY_STATION_PHONE, "客服-站点专属电话", dto.getStationPhone());
        upsert(KEY_NIGHT_TIP, "客服-夜间提示", dto.getNightTip());

        faqMapper.logicDeleteAll();
        int order = 1;
        if (dto.getFaqs() != null) {
            for (CsFaq f : dto.getFaqs()) {
                if (f.getQuestion() == null || f.getQuestion().isBlank()) {
                    continue;
                }
                WashCsFaq e = new WashCsFaq();
                e.setQuestion(f.getQuestion());
                e.setAnswer(f.getAnswer() == null ? "" : f.getAnswer());
                e.setSortOrder(order++);
                faqMapper.insert(e);
            }
        }
        return AjaxResult.success("保存成功");
    }

    /** sys_config 优先；未配置（null/空串）回落 yml 值，保证后台看到的是当前生效值 */
    private String resolve(String key, String fallback) {
        String value = sysConfigService.selectConfigByKey(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    /** sys_config 已存在则更新，否则新增（自定义参数 config_type='N'） */
    private void upsert(String key, String name, String value) {
        SysConfig probe = new SysConfig();
        probe.setConfigKey(key);
        List<SysConfig> list = sysConfigService.selectConfigList(probe);
        if (list != null && !list.isEmpty()) {
            SysConfig existing = list.get(0);
            existing.setConfigValue(value == null ? "" : value);
            sysConfigService.updateConfig(existing);
        } else {
            SysConfig c = new SysConfig();
            c.setConfigName(name);
            c.setConfigKey(key);
            c.setConfigValue(value == null ? "" : value);
            c.setConfigType("N");
            sysConfigService.insertConfig(c);
        }
    }
}
