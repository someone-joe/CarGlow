package com.ruoyi.wash.pay.dto;

/** 我的钱包（余额）。单位：分。 */
public class WalletVO {
    /** 充值本金余额（分） */
    private Long balance;
    /** 赠送余额（分） */
    private Long giftBalance;
    /** 累计充值本金（分） */
    private Long totalRecharge;

    public WalletVO() {
    }

    public WalletVO(Long balance, Long giftBalance, Long totalRecharge) {
        this.balance = balance;
        this.giftBalance = giftBalance;
        this.totalRecharge = totalRecharge;
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
}
