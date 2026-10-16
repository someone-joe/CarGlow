package com.ruoyi.wash.notify.transport;

import java.util.Map;

/**
 * 订阅消息发送通道。
 *
 * <p>骨架期只有 {@link MockNotifyTransport}（打点）；接真实微信时新增实现并切换，业务只依赖本接口。
 */
public interface NotifyTransport {

    /**
     * @param openid     接收人 openid
     * @param templateId 微信模板ID
     * @param data       模板变量（thing1/character_string2 等微信字段）
     */
    void send(String openid, String templateId, Map<String, String> data);
}
