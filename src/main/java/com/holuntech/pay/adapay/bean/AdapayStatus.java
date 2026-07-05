package com.holuntech.pay.adapay.bean;

/**
 * SDK层统一交易状态。
 *
 * @author egan
 */
public enum AdapayStatus {

    PAY_SUCCESS("PAY_SUCCESS", "支付成功"),
    PAY_PROCESSING("PAY_PROCESSING", "支付中"),
    PAY_FAILED("PAY_FAILED", "支付失败"),
    CLOSED("CLOSED", "已关闭"),
    REFUND_SUCCESS("REFUND_SUCCESS", "退款成功"),
    REFUND_PROCESSING("REFUND_PROCESSING", "退款中"),
    REFUND_FAILED("REFUND_FAILED", "退款失败"),
    UNKNOWN("UNKNOWN", "未知状态");

    private final String code;
    private final String description;

    AdapayStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static AdapayStatus payment(String status) {
        if ("succeeded".equalsIgnoreCase(status) || "S".equalsIgnoreCase(status)) {
            return PAY_SUCCESS;
        }
        if ("pending".equalsIgnoreCase(status) || "P".equalsIgnoreCase(status) || "I".equalsIgnoreCase(status)) {
            return PAY_PROCESSING;
        }
        if ("failed".equalsIgnoreCase(status) || "F".equalsIgnoreCase(status)) {
            return PAY_FAILED;
        }
        if ("closed".equalsIgnoreCase(status) || "close".equalsIgnoreCase(status)) {
            return CLOSED;
        }
        return UNKNOWN;
    }

    public static AdapayStatus refund(String status) {
        if ("succeeded".equalsIgnoreCase(status) || "S".equalsIgnoreCase(status)) {
            return REFUND_SUCCESS;
        }
        if ("pending".equalsIgnoreCase(status) || "P".equalsIgnoreCase(status) || "I".equalsIgnoreCase(status)) {
            return REFUND_PROCESSING;
        }
        if ("failed".equalsIgnoreCase(status) || "F".equalsIgnoreCase(status)) {
            return REFUND_FAILED;
        }
        return UNKNOWN;
    }

    public static AdapayStatus from(String eventType, String status, String object) {
        if (eventType != null) {
            if (eventType.startsWith("refund.")) {
                return refund(status);
            }
            if ("payment.close.succeeded".equals(eventType)) {
                return CLOSED;
            }
            if ("payment.close.failed".equals(eventType)) {
                return PAY_FAILED;
            }
        }
        if ("refund".equalsIgnoreCase(object)) {
            return refund(status);
        }
        return payment(status);
    }
}
