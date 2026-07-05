package com.holuntech.pay.adapay.bean;

import com.egzosn.pay.common.bean.TransactionType;

/**
 * Adapay交易类型
 * 基于Adapay官方支持的支付渠道
 *
 * @author egan
 */
public enum AdapayTransactionType implements TransactionType {

    /**
     * 支付宝扫码支付
     */
    ALIPAY_QR("alipay_qr", "支付宝扫码支付"),

    /**
     * 支付宝WAP支付
     */
    ALIPAY_WAP("alipay_wap", "支付宝WAP支付"),

    /**
     * 支付宝APP支付
     */
    ALIPAY_APP("alipay_app", "支付宝APP支付"),

    /**
     * 支付宝PC网页支付
     */
    ALIPAY_PAGE("alipay_page", "支付宝PC网页支付"),

    /**
     * 支付宝小程序支付
     */
    ALIPAY_LITE("alipay_lite", "支付宝小程序支付"),

    /**
     * 微信扫码支付
     */
    WX_PUB_QR("wx_pub_qr", "微信扫码支付"),

    /**
     * 微信公众号支付
     */
    WX_PUB("wx_pub", "微信公众号支付"),

    /**
     * 微信WAP支付
     */
    WX_WAP("wx_wap", "微信WAP支付"),

    /**
     * 微信APP支付
     */
    WX_APP("wx_app", "微信APP支付"),

    /**
     * 微信小程序支付
     */
    WX_LITE("wx_lite", "微信小程序支付"),

    /**
     * 银联扫码支付
     */
    UNION_QR("union_qr", "银联扫码支付"),

    /**
     * 银联WAP支付
     */
    UNION_WAP("union_wap", "银联WAP支付"),

    /**
     * 银联APP支付
     */
    UNION_APP("union_app", "银联APP支付"),

    /**
     * 收银台支付 (Checkout)
     * 由Adapay提供统一收银台页面，支持多种支付方式
     */
    CHECKOUT("checkout", "收银台支付"),

    /**
     * 快捷支付
     */
    FAST_PAY("fast_pay", "快捷支付"),

    /**
     * 支付查询
     */
    QUERY("query", "支付查询"),

    /**
     * 退款
     */
    REFUND("refund", "退款"),

    /**
     * 退款查询
     */
    REFUND_QUERY("refund_query", "退款查询"),

    /**
     * 关闭订单
     */
    CLOSE("close", "关闭订单"),

    /**
     * 账单查询
     */
    BILL("bill", "账单查询");

    /**
     * 交易类型代码
     */
    private final String code;

    /**
     * 交易类型描述
     */
    private final String description;

    AdapayTransactionType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String getType() {
        return code;
    }

    @Override
    public String getMethod() {
        return code;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
