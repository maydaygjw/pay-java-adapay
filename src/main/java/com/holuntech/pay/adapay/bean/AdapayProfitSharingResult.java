package com.holuntech.pay.adapay.bean;

import com.alibaba.fastjson.JSON;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Adapay 分账确认结果（延时分账确认）。
 *
 * @author egan
 */
public class AdapayProfitSharingResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 支付确认对象 id
     */
    private String id;

    /**
     * 关联的 payment_id
     */
    private String paymentId;

    /**
     * 商户请求订单号
     */
    private String orderNo;

    /**
     * 确认金额
     */
    private BigDecimal confirmAmt;

    /**
     * 手续费金额
     */
    private BigDecimal feeAmt;

    /**
     * 已确认金额
     */
    private BigDecimal confirmedAmt;

    /**
     * 已退款金额
     */
    private BigDecimal refundedAmt;

    /**
     * 状态
     */
    private String status;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误描述
     */
    private String errorMsg;

    /**
     * 原始响应属性
     */
    private Map<String, Object> attrs;

    public AdapayProfitSharingResult() {
    }

    public AdapayProfitSharingResult(Map<String, Object> result) {
        this.id = getString(result, "id");
        this.paymentId = getString(result, "payment_id");
        this.orderNo = getString(result, "order_no");
        this.confirmAmt = toBigDecimal(getString(result, "confirm_amt"));
        this.feeAmt = toBigDecimal(getString(result, "fee_amt"));
        this.confirmedAmt = toBigDecimal(getString(result, "confirmed_amt"));
        this.refundedAmt = toBigDecimal(getString(result, "refunded_amt"));
        this.status = getString(result, "status");
        this.errorCode = getString(result, "error_code");
        this.errorMsg = getString(result, "error_msg");
        this.attrs = result;
    }

    public String getCode() {
        return status;
    }

    public String getMsg() {
        if (errorMsg != null && !errorMsg.isEmpty()) {
            return errorMsg;
        }
        AdapayStatus statusEnum = getAdapayStatus();
        return statusEnum == null ? null : statusEnum.getDescription();
    }

    public String getResultCode() {
        return errorCode;
    }

    public String getResultMsg() {
        return errorMsg;
    }

    public String getTradeNo() {
        return paymentId;
    }

    public String getOutTradeNo() {
        return orderNo;
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public void setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
    }

    public Object getAttr(String key) {
        return attrs == null ? null : attrs.get(key);
    }

    public String getAttrString(String key) {
        Object value = getAttr(key);
        return value == null ? null : value.toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public BigDecimal getConfirmAmt() {
        return confirmAmt;
    }

    public void setConfirmAmt(BigDecimal confirmAmt) {
        this.confirmAmt = confirmAmt;
    }

    public void setConfirmAmtString(String confirmAmt) {
        this.confirmAmt = toBigDecimal(confirmAmt);
    }

    public BigDecimal getFeeAmt() {
        return feeAmt;
    }

    public void setFeeAmt(BigDecimal feeAmt) {
        this.feeAmt = feeAmt;
    }

    public void setFeeAmtString(String feeAmt) {
        this.feeAmt = toBigDecimal(feeAmt);
    }

    public BigDecimal getConfirmedAmt() {
        return confirmedAmt;
    }

    public void setConfirmedAmt(BigDecimal confirmedAmt) {
        this.confirmedAmt = confirmedAmt;
    }

    public void setConfirmedAmtString(String confirmedAmt) {
        this.confirmedAmt = toBigDecimal(confirmedAmt);
    }

    public BigDecimal getRefundedAmt() {
        return refundedAmt;
    }

    public void setRefundedAmt(BigDecimal refundedAmt) {
        this.refundedAmt = refundedAmt;
    }

    public void setRefundedAmtString(String refundedAmt) {
        this.refundedAmt = toBigDecimal(refundedAmt);
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

    public AdapayStatus getAdapayStatus() {
        return AdapayStatus.profitSharing(status);
    }

    public boolean isSuccess() {
        return AdapayStatus.SHARE_SUCCESS == getAdapayStatus();
    }

    public boolean isProcessing() {
        return AdapayStatus.SHARE_PROCESSING == getAdapayStatus();
    }

    public boolean isFailed() {
        return AdapayStatus.SHARE_FAILED == getAdapayStatus();
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    private static String getString(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }

    private static BigDecimal toBigDecimal(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return new BigDecimal(value);
    }
}
