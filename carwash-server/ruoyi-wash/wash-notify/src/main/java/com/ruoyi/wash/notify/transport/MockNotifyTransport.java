package com.ruoyi.wash.notify.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/** 本地开发通道：不真正调用微信，只打点，便于联调与演示。 */
@Service
public class MockNotifyTransport implements NotifyTransport {

    private static final Logger log = LoggerFactory.getLogger(MockNotifyTransport.class);

    @Override
    public void send(String openid, String templateId, Map<String, String> data) {
        log.info("[订阅消息mock] openid={} templateId={} data={}", openid, templateId, data);
    }
}
