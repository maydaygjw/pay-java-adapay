package com.holuntech.pay.adapay.bean;

import com.alibaba.fastjson.JSON;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Adapay 分账对象信息。
 * 用于实时分账或延时分账确认时传入 div_members 数组。
 *
 * @author egan
 */
public class AdapayDivMember implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分账对象 member_id，商户自己分账可传 "0"
     */
    private String memberId;

    /**
     * 分账金额，必须大于 0，保留两位小数
     */
    private BigDecimal amount;

    /**
     * 是否手续费承担方：Y-是，N-否
     * 一次分账中必须且只能有一个手续费承担方
     */
    private String feeFlag;

    public AdapayDivMember() {
    }

    public AdapayDivMember(String memberId, BigDecimal amount, String feeFlag) {
        this.memberId = memberId;
        this.amount = amount;
        this.feeFlag = feeFlag;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getFeeFlag() {
        return feeFlag;
    }

    public void setFeeFlag(String feeFlag) {
        this.feeFlag = feeFlag;
    }

    /**
     * 转换为 SDK 所需的 Map 形式。
     */
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<String, Object>(4);
        map.put("member_id", memberId);
        if (amount != null) {
            map.put("amount", amount.setScale(2, BigDecimal.ROUND_HALF_UP).toString());
        }
        map.put("fee_flag", feeFlag);
        return map;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
