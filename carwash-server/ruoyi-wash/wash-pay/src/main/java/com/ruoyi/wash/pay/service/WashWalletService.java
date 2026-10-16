package com.ruoyi.wash.pay.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.pay.domain.WashMemberWallet;
import com.ruoyi.wash.pay.domain.WashWalletFlow;
import com.ruoyi.wash.pay.domain.WashWalletGiftRule;
import com.ruoyi.wash.pay.domain.WashWalletRecharge;
import com.ruoyi.wash.pay.dto.GiftRuleVO;
import com.ruoyi.wash.pay.dto.RechargeRequest;
import com.ruoyi.wash.pay.dto.RechargeVO;
import com.ruoyi.wash.pay.dto.WalletFlowVO;
import com.ruoyi.wash.pay.dto.WalletVO;
import com.ruoyi.wash.pay.mapper.WashMemberWalletMapper;
import com.ruoyi.wash.pay.mapper.WashWalletFlowMapper;
import com.ruoyi.wash.pay.mapper.WashWalletGiftRuleMapper;
import com.ruoyi.wash.pay.mapper.WashWalletRechargeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 储值钱包业务：余额 / 充值（含赠送）/ 流水 / 订单余额抵扣。
 *
 * <p>金额单位一律「分」。扣款顺序：先赠后本。所有写操作幂等：
 * 充值到账靠 recharge 状态 INIT→SUCCESS 条件更新，订单抵扣由调用方在事务内串行推进。
 */
@Service
public class WashWalletService {

    private static final Logger log = LoggerFactory.getLogger(WashWalletService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long RECHARGE_EXPIRE_MS = 30 * 60 * 1000L;

    @Autowired
    private WashMemberWalletMapper walletMapper;
    @Autowired
    private WashWalletRechargeMapper rechargeMapper;
    @Autowired
    private WashWalletFlowMapper flowMapper;
    @Autowired
    private WashWalletGiftRuleMapper giftRuleMapper;

    // ---------------- C 端查询 ----------------

    /** 我的钱包（余额）。无钱包行时惰性创建。 */
    public WalletVO getWallet(Long memberId) {
        return toVO(ensureWallet(memberId));
    }

    /** 生效的充值赠送规则（供「充值更优惠」弹窗）。 */
    public List<GiftRuleVO> activeRules() {
        List<GiftRuleVO> out = new ArrayList<>();
        for (WashWalletGiftRule r : giftRuleMapper.selectActive()) {
            out.add(new GiftRuleVO(r.getRuleId(), r.getRechargeFen(), r.getGiftFen()));
        }
        return out;
    }

    /** 钱包流水（分页，最新在前）。 */
    public Map<String, Object> flows(Long memberId, int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 50) pageSize = 20;
        int offset = (pageNum - 1) * pageSize;
        List<WalletFlowVO> list = new ArrayList<>();
        for (WashWalletFlow f : flowMapper.selectByMember(memberId, offset, pageSize)) {
            list.add(toFlowVO(f));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", flowMapper.countByMember(memberId));
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        return data;
    }

    // ---------------- C 端充值 ----------------

    /** 发起充值：建 INIT 充值单。微信渠道后补，本地由 mockPayRecharge 触发到账。 */
    @Transactional(rollbackFor = Exception.class)
    public RechargeVO recharge(Long memberId, RechargeRequest req) {
        Long payAmount;
        Long giftAmount = 0L;
        Long ruleId = null;
        if (req.getRuleId() != null) {
            WashWalletGiftRule rule = giftRuleMapper.selectById(req.getRuleId());
            if (rule == null || !"Y".equals(rule.getEnabled())) {
                throw new ApiException(ErrorCode.A0001, "赠送档位不存在或已下架");
            }
            ruleId = rule.getRuleId();
            payAmount = rule.getRechargeFen();
            giftAmount = rule.getGiftFen() == null ? 0L : rule.getGiftFen();
        } else if (req.getAmountFen() != null && req.getAmountFen() > 0) {
            payAmount = req.getAmountFen();
        } else {
            throw new ApiException(ErrorCode.A0001, "请选择充值档位或输入金额");
        }
        WashWalletRecharge recharge = new WashWalletRecharge();
        recharge.setRechargeNo(generateNo("RC"));
        recharge.setMemberId(memberId);
        recharge.setRuleId(ruleId);
        recharge.setPayAmount(payAmount);
        recharge.setGiftAmount(giftAmount);
        recharge.setChannel("MOCK");
        recharge.setPayExpireAt(System.currentTimeMillis() + RECHARGE_EXPIRE_MS);
        rechargeMapper.insert(recharge);
        return new RechargeVO(recharge.getRechargeNo(), payAmount, giftAmount, recharge.getPayExpireAt());
    }

    /** 充值到账（本地 mock 支付触发）。幂等：重复调用返回当前余额，不重复入账。 */
    @Transactional(rollbackFor = Exception.class)
    public WalletVO mockPayRecharge(Long memberId, String rechargeNo) {
        WashWalletRecharge recharge = rechargeMapper.selectByRechargeNo(rechargeNo);
        if (recharge == null) {
            throw new ApiException(ErrorCode.A0001, "充值单不存在");
        }
        if (!recharge.getMemberId().equals(memberId)) {
            throw new ApiException(ErrorCode.A0003, "无权操作他人充值单");
        }
        if (!"INIT".equals(recharge.getStatus())) {
            return toVO(ensureWallet(memberId));
        }
        int n = rechargeMapper.markSuccess(rechargeNo, "MOCK-" + rechargeNo);
        if (n == 0) {
            return toVO(ensureWallet(memberId));
        }
        WashMemberWallet wallet = ensureWalletForUpdate(memberId);
        wallet.setBalance(wallet.getBalance() + recharge.getPayAmount());
        wallet.setGiftBalance(wallet.getGiftBalance() + (recharge.getGiftAmount() == null ? 0L : recharge.getGiftAmount()));
        wallet.setTotalRecharge(wallet.getTotalRecharge() + recharge.getPayAmount());
        walletMapper.updateBalance(wallet);
        writeFlow(memberId, "RECHARGE", recharge.getPayAmount(), 0L, wallet, rechargeNo, "储值充值");
        if (recharge.getGiftAmount() != null && recharge.getGiftAmount() > 0) {
            writeFlow(memberId, "GIFT", 0L, recharge.getGiftAmount(), wallet, rechargeNo, "充值赠送");
        }
        log.info("[储值到账] rechargeNo={} memberId={} pay={} gift={}", rechargeNo, memberId, recharge.getPayAmount(), recharge.getGiftAmount());
        return toVO(wallet);
    }

    // ---------------- 订单余额抵扣（T2，由 WashPayService 在事务内调用） ----------------

    /**
     * 用余额抵扣订单（先赠后本）。调用方需已在事务内，且订单状态为 WAIT_PAY。
     * 余额不足抛 B3004。重复调用由订单状态机兜底（非 WAIT_PAY 不再进入）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deductForOrder(Long memberId, String orderNo, long amountFen) {
        WashMemberWallet wallet = walletMapper.selectByMemberIdForUpdate(memberId);
        long balance = wallet == null ? 0L : (wallet.getBalance() == null ? 0L : wallet.getBalance());
        long gift = wallet == null ? 0L : (wallet.getGiftBalance() == null ? 0L : wallet.getGiftBalance());
        if (balance + gift < amountFen) {
            throw new ApiException(ErrorCode.B3004, "储值余额不足");
        }
        long giftDeduct = Math.min(gift, amountFen);
        long principalDeduct = amountFen - giftDeduct;
        wallet.setBalance(balance - principalDeduct);
        wallet.setGiftBalance(gift - giftDeduct);
        walletMapper.updateBalance(wallet);
        writeFlow(memberId, "PAY", -principalDeduct, -giftDeduct, wallet, orderNo, "订单余额支付");
    }

    /**
     * 订单取消退回余额（自助洗车等场景）：按原实付金额退回本金余额 + REFUND 流水。
     * 调用方需已在事务内且已校验订单归属；幂等由调用方状态机保证（仅取消一次）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void refundToWallet(Long memberId, String orderNo, long amountFen) {
        WashMemberWallet wallet = ensureWalletForUpdate(memberId);
        wallet.setBalance(wallet.getBalance() + amountFen);
        walletMapper.updateBalance(wallet);
        writeFlow(memberId, "REFUND", amountFen, 0L, wallet, orderNo, "订单取消退回");
    }

    // ---------------- 后台：赠送规则 ----------------

    /** 全部赠送规则（含停用），后台编辑用。 */
    public List<WashWalletGiftRule> listAllRules() {
        return giftRuleMapper.selectAll();
    }

    /** 整体覆盖赠送规则。充值单已存金额快照，删旧规则不影响历史。 */
    @Transactional(rollbackFor = Exception.class)
    public void saveRules(List<GiftRuleVO> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new ApiException(ErrorCode.A0001, "至少保留一条赠送规则");
        }
        for (GiftRuleVO vo : rules) {
            if (vo.getRechargeFen() == null || vo.getRechargeFen() <= 0) {
                throw new ApiException(ErrorCode.A0001, "充值金额必须大于 0");
            }
            if (vo.getGiftFen() == null || vo.getGiftFen() < 0) {
                throw new ApiException(ErrorCode.A0001, "赠送金额不能为负");
            }
        }
        giftRuleMapper.deleteAll();
        int order = 1;
        for (GiftRuleVO vo : rules) {
            WashWalletGiftRule r = new WashWalletGiftRule();
            r.setRechargeFen(vo.getRechargeFen());
            r.setGiftFen(vo.getGiftFen());
            r.setEnabled("Y");
            r.setSortOrder(order++);
            giftRuleMapper.insert(r);
        }
    }

    // ---------------- 后台：充值流水分页 ----------------

    public Map<String, Object> rechargePage(Long memberId, String status, int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("hasMember", memberId != null);
        params.put("status", status);
        params.put("hasStatus", status != null && !status.isBlank());
        params.put("offset", (pageNum - 1) * pageSize);
        params.put("limit", pageSize);
        Map<String, Object> data = new HashMap<>();
        data.put("rows", rechargeMapper.selectPage(params));
        data.put("total", rechargeMapper.selectCount(params));
        return data;
    }

    // ---------------- 内部 ----------------

    private WashMemberWallet ensureWallet(Long memberId) {
        WashMemberWallet wallet = walletMapper.selectByMemberId(memberId);
        if (wallet != null) {
            return wallet;
        }
        try {
            walletMapper.insertWallet(memberId);
        } catch (DuplicateKeyException e) {
            // 并发首访，忽略，走下面查询
        }
        wallet = walletMapper.selectByMemberId(memberId);
        if (wallet == null) {
            throw new ApiException(ErrorCode.A0005, "钱包初始化失败");
        }
        return wallet;
    }

    private WashMemberWallet ensureWalletForUpdate(Long memberId) {
        WashMemberWallet wallet = walletMapper.selectByMemberIdForUpdate(memberId);
        if (wallet != null) {
            return wallet;
        }
        try {
            walletMapper.insertWallet(memberId);
        } catch (DuplicateKeyException e) {
            // ignore
        }
        wallet = walletMapper.selectByMemberIdForUpdate(memberId);
        if (wallet == null) {
            throw new ApiException(ErrorCode.A0005, "钱包初始化失败");
        }
        return wallet;
    }

    private void writeFlow(Long memberId, String bizType, Long changeAmount, Long giftChange,
                           WashMemberWallet wallet, String bizNo, String remark) {
        WashWalletFlow flow = new WashWalletFlow();
        flow.setFlowNo(generateNo("WF"));
        flow.setMemberId(memberId);
        flow.setBizType(bizType);
        flow.setChangeAmount(changeAmount);
        flow.setGiftChange(giftChange);
        flow.setBalanceAfter(wallet.getBalance());
        flow.setGiftAfter(wallet.getGiftBalance());
        flow.setBizNo(bizNo);
        flow.setRemark(remark);
        flowMapper.insert(flow);
    }

    private WalletVO toVO(WashMemberWallet w) {
        return new WalletVO(
                w.getBalance() == null ? 0L : w.getBalance(),
                w.getGiftBalance() == null ? 0L : w.getGiftBalance(),
                w.getTotalRecharge() == null ? 0L : w.getTotalRecharge());
    }

    private WalletFlowVO toFlowVO(WashWalletFlow f) {
        WalletFlowVO vo = new WalletFlowVO();
        vo.setFlowNo(f.getFlowNo());
        vo.setBizType(f.getBizType());
        vo.setChangeAmount(f.getChangeAmount());
        vo.setGiftChange(f.getGiftChange());
        vo.setBalanceAfter(f.getBalanceAfter());
        vo.setGiftAfter(f.getGiftAfter());
        vo.setBizNo(f.getBizNo());
        vo.setRemark(f.getRemark());
        vo.setCreateTime(f.getCreateTime());
        return vo;
    }

    private String generateNo(String prefix) {
        String time = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT).format(new Date());
        return prefix + time + (100000 + RANDOM.nextInt(900000));
    }
}
