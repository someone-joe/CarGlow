package com.ruoyi.wash.notify.controller;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.notify.domain.NotifyTemplate;
import com.ruoyi.wash.notify.service.NotifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * C 端消息通知（骨架）。契约：openapi.yaml /api/v1/notify/**。
 *
 * <p>真实推送由各业务在服务端触发（订单状态/券到期/余额/活动），本控制器只提供模板列表与开发自测入口。
 */
@RestController
public class WxNotifyController {

    @Autowired
    private NotifyService notifyService;

    @GetMapping("/api/v1/notify/templates")
    public ApiResult<Map<String, String>> templates() {
        Map<String, String> out = new LinkedHashMap<>();
        for (NotifyTemplate t : NotifyTemplate.values()) {
            out.put(t.name(), t.getLabel());
        }
        return ApiResult.ok(out);
    }

    /** 开发自测：给当前会员发一条模板消息（x-dev-only） */
    @PostMapping("/api/v1/notify/test")
    public ApiResult<Boolean> test(@RequestBody(required = false) Map<String, String> body) {
        String name = body == null ? null : body.get("template");
        NotifyTemplate template;
        try {
            template = name == null || name.isBlank() ? NotifyTemplate.ORDER_STATUS : NotifyTemplate.of(name);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.A0001, "未知的通知模板：" + name);
        }
        Map<String, String> data = new HashMap<>();
        data.put("thing1", "CarGlow 测试消息");
        data.put("thing2", template.getLabel());
        return ApiResult.ok(notifyService.send(MemberContext.require(), template, data, null));
    }
}
