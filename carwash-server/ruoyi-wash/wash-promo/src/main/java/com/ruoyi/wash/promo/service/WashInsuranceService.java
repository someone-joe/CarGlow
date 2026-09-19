package com.ruoyi.wash.promo.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.promo.domain.WashInsurancePolicy;
import com.ruoyi.wash.promo.domain.WashInsuranceProduct;
import com.ruoyi.wash.promo.dto.InsurancePolicyVO;
import com.ruoyi.wash.promo.dto.InsuranceProductVO;
import com.ruoyi.wash.promo.mapper.WashInsurancePolicyMapper;
import com.ruoyi.wash.promo.mapper.WashInsuranceProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** C 端保险/保单：产品列表、投保（dev 模式直接生效）、我的保单。 */
@Service
public class WashInsuranceService {

    /** 保障期限（演示固定 365 天，毫秒） */
    private static final long POLICY_TERM_MS = 365L * 24 * 3600 * 1000;

    @Autowired
    private WashInsuranceProductMapper productMapper;
    @Autowired
    private WashInsurancePolicyMapper policyMapper;

    /** 可投保产品列表。 */
    public List<InsuranceProductVO> products() {
        List<InsuranceProductVO> vos = new ArrayList<>();
        for (WashInsuranceProduct p : productMapper.selectEnabled()) {
            vos.add(new InsuranceProductVO(p.getInsuranceProductId(), p.getName(),
                    p.getPriceAmount(), p.getCoverageDesc()));
        }
        return vos;
    }

    /**
     * 投保：生成保单。dev 模式（类比 mock-pay）直接置为 ACTIVE 并记实付保费，
     * 真实渠道接入时只改这里（调支付 → 支付成功回调里置 ACTIVE）。
     */
    public InsurancePolicyVO buy(Long memberId, Long productId, Long vehicleId, String orderNo, long now) {
        if (vehicleId == null) {
            throw new ApiException(ErrorCode.C2002);
        }
        WashInsuranceProduct p = productMapper.selectById(productId);
        if (p == null || !"Y".equals(p.getStatus())) {
            throw new ApiException(ErrorCode.C2001);
        }
        WashInsurancePolicy policy = new WashInsurancePolicy();
        policy.setPolicyNo("BX" + now + (1000 + (int) (Math.random() * 9000)));
        policy.setProductId(productId);
        policy.setMemberId(memberId);
        policy.setVehicleId(vehicleId);
        policy.setOrderNo(orderNo);
        policy.setStatus("ACTIVE");
        policy.setStartTime(now);
        policy.setEndTime(now + POLICY_TERM_MS);
        policy.setPaidAmount(p.getPriceAmount());
        policyMapper.insert(policy);
        return toVO(policy, p);
    }

    /** 我的保单。 */
    public List<InsurancePolicyVO> myPolicies(Long memberId) {
        List<InsurancePolicyVO> vos = new ArrayList<>();
        for (WashInsurancePolicy p : policyMapper.selectMy(memberId)) {
            WashInsuranceProduct product = productMapper.selectById(p.getProductId());
            vos.add(toVO(p, product));
        }
        return vos;
    }

    private InsurancePolicyVO toVO(WashInsurancePolicy p, WashInsuranceProduct product) {
        String name = product == null ? "" : product.getName();
        return new InsurancePolicyVO(p.getPolicyId(), p.getPolicyNo(), p.getProductId(), name,
                p.getVehicleId(), p.getOrderNo(), p.getStatus(),
                p.getStartTime(), p.getEndTime(), p.getPaidAmount());
    }
}
