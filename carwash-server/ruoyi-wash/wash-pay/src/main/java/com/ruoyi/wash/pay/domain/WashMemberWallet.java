package com.ruoyi.wash.pay.domain;

/**
 * 会员储值钱包（充值本金 + 赠送）。一行一会员，唯一键 member_id。
 *
 * <p>本期不支持提现。扣款顺序：先赠后本（deductForOrder 内实现）。
 */
public class WashMemberWallet {

    private Long walletId;
    private Long memberId;
    /** 充值本金余额（分） */
    private Long balance;
    /** 赠送余额（分） */
    private Long giftBalance;
    /** 累计充值本金（分，统计用） */
    private Long totalRecharge;
    /** 乐观锁版本 */
    private Integer version;

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public Long getGiftBalance() {
        return giftBalance;
    }

    public void setGiftBalance(Long giftBalance) {
        this.giftBalance = giftBalance;
    }

    public Long getTotalRecharge() {
        return totalRecharge;
    }

    public void setTotalRecharge(Long totalRecharge) {
        this.totalRecharge = totalRecharge;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
