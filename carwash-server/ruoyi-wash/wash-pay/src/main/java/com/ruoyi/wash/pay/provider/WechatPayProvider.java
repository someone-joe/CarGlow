package com.ruoyi.wash.pay.provider;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.pay.domain.WashRefundOrder;
import com.ruoyi.wash.pay.dto.PrepayVO;

/**
 * 微信支付渠道 —— **占位实现**，目前会明确报错，绝不"假装支付成功"。
 *
 * <p>接入前必须准备：
 * 1. 微信小程序 appid（同时配置 wash.wx.appid/secret，否则登录也是模拟的）；
 * 2. 微信支付商户号 mchId + 商户 API 证书（v3 密钥、证书序列号）；
 * 3. 已备案的 HTTPS 域名（支付结果回调地址，微信服务器要能访问到）；
 * 4. 引入 SDK：com.github.wechatpay-apiv3:wechatpay-java。
 *
 * <p>接入时把本类的方法替换为真实调用即可，业务代码（WashPayService / 状态机）无需改动。
 */
public class WechatPayProvider implements PaymentProvider {

    @Override
    public String code() {
        return "wechat";
    }

    @Override
    public PrepayVO prepay(WashOrder order) {
        throw new ApiException(ErrorCode.C0001, "微信支付尚未接入：需要商户号、API 证书与备案回调域名，详见本类注释");
    }

    @Override
    public RefundResult refund(WashRefundOrder refund) {
        throw new ApiException(ErrorCode.C0001, "微信支付尚未接入：需要商户号、API 证书与备案回调域名，详见本类注释");
    }
}
