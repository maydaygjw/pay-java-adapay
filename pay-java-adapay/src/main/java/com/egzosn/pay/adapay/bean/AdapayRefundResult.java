package com.egzosn.pay.adapay.bean;

import com.egzosn.pay.common.bean.BaseRefundResult;
import com.egzosn.pay.common.bean.CurType;
import com.egzosn.pay.common.bean.DefaultCurType;

import java.math.BigDecimal;

/**
 * Adapay退款结果
 *
 * @author egan
 */
public class AdapayRefundResult extends BaseRefundResult {

    /**
     * 退款ID
     */
    private String id;

    /**
     * 退款状态
     */
    private String status;

    /**
     * 错误代码
     */
    private String errorCode;

    /**
     * 错误消息
     */
    private String errorMsg;

    /**
     * 支付ID (平台交易号)
     */
    private String paymentId;

    /**
     * 商户退款单号
     */
    private String refundOrderNo;

    /**
     * 退款金额
     */
    private BigDecimal refundAmt;

    @Override
    public String getCode() {
        return status;
    }

    @Override
    public String getMsg() {
        return errorMsg;
    }

    @Override
    public String getResultCode() {
        return errorCode;
    }

    @Override
    public String getResultMsg() {
        return errorMsg;
    }

    @Override
    public BigDecimal getRefundFee() {
        return refundAmt;
    }

    @Override
    public CurType getRefundCurrency() {
        return DefaultCurType.CNY;
    }

    @Override
    public String getTradeNo() {
        return paymentId;
    }

    @Override
    public String getOutTradeNo() {
        // Adapay没有返回商户订单号，可以从attrs中获取
        return getAttrString("order_no");
    }

    @Override
    public String getRefundNo() {
        return refundOrderNo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getRefundOrderNo() {
        return refundOrderNo;
    }

    public void setRefundOrderNo(String refundOrderNo) {
        this.refundOrderNo = refundOrderNo;
    }

    public BigDecimal getRefundAmt() {
        return refundAmt;
    }

    public void setRefundAmt(BigDecimal refundAmt) {
        this.refundAmt = refundAmt;
    }

    /**
     * 设置退款金额(字符串)
     */
    public void setRefundAmtString(String refundAmt) {
        if (refundAmt != null && !refundAmt.isEmpty()) {
            this.refundAmt = new BigDecimal(refundAmt);
        }
    }
}
