package com.ruoyi.wash.pay.config;

import com.ruoyi.wash.pay.provider.MockPaymentProvider;
import com.ruoyi.wash.pay.provider.PaymentProvider;
import com.ruoyi.wash.pay.provider.WechatPayProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付渠道选择：wash.pay.provider = mock（默认，本地开发） / wechat（真实微信支付）。
 *
 * <p>切到 wechat 前请确认已具备商户号、API 证书与备案回调域名，
 * 否则接口会明确报错（WechatPayProvider 目前是占位实现，不会假装成功）。
 */
@Configuration
public class PayProviderConfig {

    @Bean
    @ConditionalOnProperty(name = "wash.pay.provider", havingValue = "wechat")
    public PaymentProvider wechatPayProvider() {
        return new WechatPayProvider();
    }

    @Bean
    @ConditionalOnProperty(name = "wash.pay.provider", havingValue = "mock", matchIfMissing = true)
    public PaymentProvider mockPaymentProvider() {
        return new MockPaymentProvider();
    }
}
