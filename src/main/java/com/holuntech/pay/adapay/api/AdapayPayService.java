package com.holuntech.pay.adapay.api;

import com.egzosn.pay.common.api.BasePayService;
import com.egzosn.pay.common.bean.*;
import com.egzosn.pay.common.bean.result.PayException;
import com.egzosn.pay.common.bean.result.PayError;
import com.egzosn.pay.common.exception.PayErrorException;
import com.egzosn.pay.common.http.HttpConfigStorage;
import com.egzosn.pay.common.util.DateUtils;
import com.egzosn.pay.common.util.str.StringUtils;
import com.holuntech.pay.adapay.bean.AdapayPayMessage;
import com.holuntech.pay.adapay.bean.AdapayTransactionType;
import com.holuntech.pay.adapay.bean.AdapayRefundResult;
import com.holuntech.pay.adapay.bean.AdapayStatus;
import com.huifu.adapay.core.util.AdapaySign;
import com.huifu.adapay.model.Bill;
import com.huifu.adapay.model.Payment;
import com.huifu.adapay.model.Refund;
import com.huifu.adapay.model.Checkout;
import com.huifu.adapay.core.exception.BaseAdaPayException;

import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapay支付服务
 * 基于Adapay官方SDK实现
 *
 * @author egan
 */
public class AdapayPayService extends BasePayService<AdapayPayConfigStorage> {

    /**
     * 构造函数
     *
     * @param payConfigStorage 支付配置
     */
    public AdapayPayService(AdapayPayConfigStorage payConfigStorage) {
        super(payConfigStorage);
        try {
            payConfigStorage.initAdapayConfig();
        } catch (Exception e) {
            throw new PayErrorException(new PayException("-1", "初始化Adapay配置失败: " + e.getMessage()));
        }
    }

    /**
     * 构造函数
     *
     * @param payConfigStorage 支付配置
     * @param httpConfigStorage HTTP配置
     */
    public AdapayPayService(AdapayPayConfigStorage payConfigStorage, HttpConfigStorage httpConfigStorage) {
        super(payConfigStorage, httpConfigStorage);
        try {
            payConfigStorage.initAdapayConfig();
        } catch (Exception e) {
            throw new PayErrorException(new PayException("-1", "初始化Adapay配置失败: " + e.getMessage()));
        }
    }

    /**
     * 获取支付请求地址
     * Adapay使用SDK方式调用，不需要直接访问URL
     *
     * @param transactionType 交易类型
     * @return 空字符串
     */
    @Override
    public String getReqUrl(TransactionType transactionType) {
        return payConfigStorage.getApiBase();
    }

    /**
     * 签名验证。
     * Adapay异步通知签名原文为data字段，签名字段为sign，算法为SHA1withRSA。
     *
     * @param params 待验证的参数
     * @param sign 签名
     * @return 验证结果
     */
    public boolean signVerify(Map<String, Object> params, String sign) {
        if (params == null) {
            return false;
        }
        String data = getString(params, "data");
        String signValue = StringUtils.isNotEmpty(sign) ? sign : getString(params, "sign");
        String publicKey = payConfigStorage.getRsaPublicKey();
        if (StringUtils.isEmpty(publicKey)) {
            publicKey = payConfigStorage.getKeyPublic();
        }
        if (StringUtils.isEmpty(data) || StringUtils.isEmpty(signValue) || StringUtils.isEmpty(publicKey)) {
            return false;
        }
        try {
            String charset = StringUtils.isEmpty(payConfigStorage.getInputCharset()) ? "UTF-8" : payConfigStorage.getInputCharset();
            return AdapaySign.verifySign(data, signValue, publicKey, charset);
        } catch (Exception e) {
            LOG.warn("Adapay callback sign verify failed", e);
            return false;
        }
    }

    /**
     * 生成并设置签名
     * Adapay SDK内部会自动处理签名
     *
     * @param parameters 请求参数
     * @param characterEncoding 字符编码
     * @return 带签名的参数
     */
    public Map<String, Object> setSign(Map<String, Object> parameters, String characterEncoding) {
        // Adapay SDK会自动处理签名，这里直接返回
        return parameters;
    }

    /**
     * 回调校验
     *
     * @param params 回调参数
     * @return 是否验证成功
     */
    public boolean verify(Map<String, Object> params) {
        return signVerify(params, getString(params, "sign"));
    }

    /**
     * 回调校验
     *
     * @param noticeParams 回调参数
     * @return 是否验证成功
     */
    @Override
    public boolean verify(NoticeParams noticeParams) {
        if (noticeParams == null) {
            return false;
        }
        return verify(noticeParams.getBody());
    }

    /**
     * 支付宝页面跳转同步通知页面路径
     *
     * @param params 参数
     * @return 支付方返回的信息转换后的内容
     */
    public PayOutMessage getPayOutMessage(Map<String, Object> params) {
        return PayOutMessage.TEXT().content("success").build();
    }

    /**
     * 获取输出消息，用于返回给支付端, 告知支付状态
     *
     * @param code 状态码
     * @param message 消息
     * @return 返回输出消息
     */
    @Override
    public PayOutMessage getPayOutMessage(String code, String message) {
        return PayOutMessage.TEXT().content(code).build();
    }

    /**
     * 获取成功输出消息
     *
     * @param payMessage 支付回调消息
     * @return 返回输出消息
     */
    @Override
    public PayOutMessage successPayOutMessage(PayMessage payMessage) {
        return PayOutMessage.TEXT().content("success").build();
    }

    /**
     * 创建支付订单 - 统一支付接口
     *
     * @param order 支付订单信息
     * @return 支付结果信息（包含支付URL、二维码等）
     */
    @Override
    public Map<String, Object> orderInfo(PayOrder order) {
        final Map<String, Object> params = buildPaymentParams(order);

        // 根据交易类型选择不同的支付方式
        String payChannel = order.getTransactionType().getType();
        params.put("pay_channel", payChannel);

        Map<String, Object> result = executeWithConfig(new AdapayInvoker<Map<String, Object>>() {
            @Override
            public Map<String, Object> invoke() throws Exception {
                return Payment.create(params, payConfigStorage.getMerchantKey());
            }
        }, "创建Adapay支付订单失败");
        enrichPaymentStatus(result, null);
        return result;
    }

    /**
     * 收银台支付 - 由Adapay提供统一收银台页面
     *
     * @param order 支付订单
     * @return 返回收银台URL
     */
    public Map<String, Object> checkoutPay(PayOrder order) {
        final Map<String, Object> params = buildPaymentParams(order);

        Map<String, Object> result = executeWithConfig(new AdapayInvoker<Map<String, Object>>() {
            @Override
            public Map<String, Object> invoke() throws Exception {
                return Checkout.create(params, payConfigStorage.getMerchantKey());
            }
        }, "创建Adapay收银台支付失败");
        enrichPaymentStatus(result, null);
        return result;
    }

    /**
     * 构建支付参数
     *
     * @param order 支付订单
     * @return 支付参数Map
     */
    private Map<String, Object> buildPaymentParams(PayOrder order) {
        Map<String, Object> params = new HashMap<>(16);
        
        // 必填参数
        params.put("app_id", payConfigStorage.getAppId());
        params.put("order_no", order.getOutTradeNo());
        params.put("pay_amt", order.getPrice().toString());
        params.put("goods_title", order.getSubject());
        
        // 可选参数
        if (order.getBody() != null) {
            params.put("goods_desc", order.getBody());
        }
        
        if (payConfigStorage.getNotifyUrl() != null) {
            params.put("notify_url", payConfigStorage.getNotifyUrl());
        }
        
        if (payConfigStorage.getReturnUrl() != null) {
            params.put("return_url", payConfigStorage.getReturnUrl());
        }
        
        // 币种，默认人民币
        params.put("currency", "cny");
        
        // 其他扩展参数
        if (order.getAttrs() != null && !order.getAttrs().isEmpty()) {
            params.putAll(order.getAttrs());
        }
        
        return params;
    }

    /**
     * 查询支付订单状态
     *
     * @param tradeNo 平台订单号
     * @param outTradeNo 商户订单号
     * @return 查询结果
     */
    @Override
    public Map<String, Object> query(String tradeNo, String outTradeNo) {
        if (StringUtils.isNotEmpty(tradeNo)) {
            final String paymentId = tradeNo;
            Map<String, Object> result = executeWithConfig(new AdapayInvoker<Map<String, Object>>() {
                @Override
                public Map<String, Object> invoke() throws Exception {
                    return Payment.query(paymentId, payConfigStorage.getMerchantKey());
                }
            }, "查询Adapay订单失败");
            enrichPaymentStatus(result, null);
            return result;
        }

        if (StringUtils.isNotEmpty(outTradeNo)) {
            return queryByOrderNo(outTradeNo);
        }

        throw new PayErrorException(new PayException("-1", "查询Adapay订单失败: payment_id和order_no不能同时为空"));
    }

    /**
     * 查询支付订单状态(支持AssistOrder)
     *
     * @param assistOrder 辅助订单
     * @return 查询结果
     */
    @Override
    public Map<String, Object> query(AssistOrder assistOrder) {
        return query(assistOrder.getTradeNo(), assistOrder.getOutTradeNo());
    }

    /**
     * 关闭订单
     *
     * @param tradeNo 平台订单号
     * @param outTradeNo 商户订单号
     * @return 关闭结果
     */
    @Override
    public Map<String, Object> close(String tradeNo, String outTradeNo) {
        final String paymentId = resolvePaymentId(tradeNo, outTradeNo);
        final Map<String, Object> params = new HashMap<>(4);
        params.put("payment_id", paymentId);
        if (StringUtils.isNotEmpty(payConfigStorage.getNotifyUrl())) {
            params.put("notify_url", payConfigStorage.getNotifyUrl());
        }

        Map<String, Object> result = executeWithConfig(new AdapayInvoker<Map<String, Object>>() {
            @Override
            public Map<String, Object> invoke() throws Exception {
                return Payment.close(params, payConfigStorage.getMerchantKey());
            }
        }, "关闭Adapay订单失败");
        enrichStatus(result, AdapayStatus.CLOSED);
        return result;
    }

    /**
     * 申请退款
     *
     * @param refundOrder 退款订单信息
     * @return 退款结果
     */
    @Override
    public AdapayRefundResult refund(RefundOrder refundOrder) {
        final String paymentId = resolvePaymentId(refundOrder.getTradeNo(), refundOrder.getOutTradeNo());
        final Map<String, Object> params = new HashMap<>(8);
        params.put("app_id", payConfigStorage.getAppId());
        params.put("payment_id", paymentId);
        params.put("refund_order_no", refundOrder.getRefundNo());
        params.put("refund_amt", refundOrder.getRefundAmount().toString());

        if (refundOrder.getDescription() != null) {
            params.put("reason", refundOrder.getDescription());
        }

        if (StringUtils.isNotEmpty(refundOrder.getNotifyUrl())) {
            params.put("notify_url", refundOrder.getNotifyUrl());
        } else if (StringUtils.isNotEmpty(payConfigStorage.getNotifyUrl())) {
            params.put("notify_url", payConfigStorage.getNotifyUrl());
        }

        Map<String, Object> result = executeWithConfig(new AdapayInvoker<Map<String, Object>>() {
            @Override
            public Map<String, Object> invoke() throws Exception {
                return Refund.create(paymentId, params, payConfigStorage.getMerchantKey());
            }
        }, "Adapay退款失败");
        enrichRefundStatus(result);

        AdapayRefundResult refundResult = new AdapayRefundResult();
        refundResult.setId(getString(result, "id"));
        refundResult.setStatus(getString(result, "status"));
        refundResult.setRefundOrderNo(getString(result, "refund_order_no"));
        refundResult.setPaymentId(getString(result, "payment_id"));
        refundResult.setRefundAmtString(getString(result, "refund_amt"));
        refundResult.setErrorCode(getString(result, "error_code"));
        refundResult.setErrorMsg(getString(result, "error_msg"));
        refundResult.setAttrs(result);

        return refundResult;
    }

    /**
     * 关闭订单(支持AssistOrder)
     *
     * @param assistOrder 辅助订单
     * @return 关闭结果
     */
    @Override
    public Map<String, Object> close(AssistOrder assistOrder) {
        return close(assistOrder.getTradeNo(), assistOrder.getOutTradeNo());
    }

    /**
     * 查询退款
     *
     * @param refundOrder 退款订单
     * @return 退款查询结果
     */
    @Override
    public Map<String, Object> refundquery(RefundOrder refundOrder) {
        final Map<String, Object> params = new HashMap<String, Object>(4);
        if (StringUtils.isNotEmpty(refundOrder.getRefundNo())) {
            params.put("refund_order_no", refundOrder.getRefundNo());
        }
        if (StringUtils.isNotEmpty(refundOrder.getTradeNo())) {
            params.put("payment_id", refundOrder.getTradeNo());
        }
        Object refundId = refundOrder.getAttr("refund_id");
        if (refundId != null) {
            params.put("refund_id", refundId);
        }
        if (params.isEmpty()) {
            throw new PayErrorException(new PayException("-1", "查询Adapay退款失败: refund_id、payment_id、refund_order_no不能同时为空"));
        }

        Map<String, Object> result = executeWithConfig(new AdapayInvoker<Map<String, Object>>() {
            @Override
            public Map<String, Object> invoke() throws Exception {
                return Refund.query(params, payConfigStorage.getMerchantKey());
            }
        }, "查询Adapay退款失败");
        enrichRefundStatus(result);
        return result;
    }

    /**
     * 下载对账单
     *
     * @param billDate 账单时间
     * @param billType 账单类型
     * @return 账单内容
     */
    @Override
    public Map<String, Object> downloadBill(Date billDate, BillType billType) {
        final Map<String, Object> params = new HashMap<>(4);
        String datePattern = DateUtils.YYYYMMDD;
        if (billType != null && StringUtils.isNotEmpty(billType.getDatePattern())) {
            datePattern = billType.getDatePattern();
        }
        params.put("bill_date", DateUtils.formatDate(billDate, datePattern));

        // Adapay将特殊账单能力通过自定义功能号区分，例如余额支付账单。
        if (billType != null && StringUtils.isNotEmpty(billType.getCustom())) {
            params.put("adapay_func_code", billType.getCustom());
        }

        return executeWithConfig(new AdapayInvoker<Map<String, Object>>() {
            @Override
            public Map<String, Object> invoke() throws Exception {
                return Bill.download(params, payConfigStorage.getMerchantKey());
            }
        }, "下载Adapay账单失败");
    }

    /**
     * 获取二维码支付链接
     *
     * @param order 支付订单
     * @return 二维码内容或链接
     */
    @Override
    public <O extends PayOrder> String getQrPay(O order) {
        // 如果订单没有指定交易类型，默认使用支付宝扫码支付
        if (order.getTransactionType() == null) {
            order.setTransactionType(AdapayTransactionType.ALIPAY_QR);
        }

        // 确保是二维码支付类型
        String payChannel = order.getTransactionType().getType();
        if (!isQrPayChannel(payChannel)) {
            throw new PayErrorException(new PayException("-1", "不支持的二维码支付类型: " + payChannel));
        }

        Map<String, Object> result = orderInfo(order);

        // 从Adapay响应中提取二维码信息
        // Adapay的响应中通常包含qr_code字段
        Object qrCode = result.get("qr_code");
        if (qrCode != null) {
            return qrCode.toString();
        }

        // 如果没有qr_code字段，尝试其他可能的字段
        Object payUrl = result.get("pay_url");
        if (payUrl != null) {
            return payUrl.toString();
        }

        throw new PayErrorException(new PayException("-1", "无法获取二维码支付信息"));
    }

    /**
     * 判断是否为二维码支付渠道
     *
     * @param payChannel 支付渠道
     * @return 是否为二维码支付
     */
    private boolean isQrPayChannel(String payChannel) {
        return "alipay_qr".equals(payChannel) ||
               "wx_pub_qr".equals(payChannel) ||
               "union_qr".equals(payChannel);
    }

    @Override
    public PayMessage createMessage(Map<String, Object> params) {
        return new AdapayPayMessage(params);
    }

    /**
     * 构建请求
     * Adapay使用SDK调用，不需要构建HTTP请求
     *
     * @param orderInfo 订单信息
     * @param method 请求方法
     * @return 请求字符串
     */
    @Override
    public String buildRequest(Map<String, Object> orderInfo, MethodType method) {
        // Adapay通过SDK调用，不需要构建HTTP表单
        // 返回订单信息的JSON字符串作为标识
        return "Adapay SDK Payment - Order Info: " + orderInfo.toString();
    }

    @Override
    public <O extends PayOrder> Map<String, Object> microPay(O order) {
        throw new PayErrorException(new PayException("-1", "Adapay暂不支持付款码/刷卡支付"));
    }

    private Map<String, Object> queryByOrderNo(final String orderNo) {
        final Map<String, Object> params = new HashMap<String, Object>(4);
        params.put("app_id", payConfigStorage.getAppId());
        params.put("order_no", orderNo);
        params.put("page_index", "1");
        params.put("page_size", "1");

        Map<String, Object> result = executeWithConfig(new AdapayInvoker<Map<String, Object>>() {
            @Override
            public Map<String, Object> invoke() throws Exception {
                return Payment.queryList(params, payConfigStorage.getMerchantKey());
            }
        }, "按order_no查询Adapay订单失败");
        enrichPaymentStatus(result, null);
        return result;
    }

    private String resolvePaymentId(String tradeNo, String outTradeNo) {
        if (StringUtils.isNotEmpty(tradeNo)) {
            return tradeNo;
        }
        if (StringUtils.isEmpty(outTradeNo)) {
            throw new PayErrorException(new PayException("-1", "payment_id为空时必须提供order_no"));
        }
        Map<String, Object> queryResult = queryByOrderNo(outTradeNo);
        String paymentId = extractPaymentId(queryResult);
        if (StringUtils.isEmpty(paymentId)) {
            throw new PayErrorException(new PayException("-1", "未能通过order_no查询到Adapay payment_id: " + outTradeNo));
        }
        return paymentId;
    }

    private String extractPaymentId(Map<String, Object> result) {
        if (result == null) {
            return null;
        }
        String paymentId = getString(result, "payment_id");
        if (StringUtils.isNotEmpty(paymentId)) {
            return paymentId;
        }
        paymentId = getString(result, "id");
        if (StringUtils.isNotEmpty(paymentId) && "payment".equals(getString(result, "object"))) {
            return paymentId;
        }
        Object payments = result.get("payments");
        if (payments instanceof List && !((List) payments).isEmpty()) {
            Object first = ((List) payments).get(0);
            if (first instanceof Map) {
                return getString((Map<String, Object>) first, "id");
            }
        }
        return null;
    }

    private void enrichPaymentStatus(Map<String, Object> result, AdapayStatus defaultStatus) {
        if (result == null) {
            return;
        }
        Object payments = result.get("payments");
        if (payments instanceof List) {
            AdapayStatus firstStatus = null;
            for (Object payment : (List) payments) {
                if (payment instanceof Map) {
                    Map<String, Object> paymentMap = (Map<String, Object>) payment;
                    AdapayStatus paymentStatus = AdapayStatus.payment(getString(paymentMap, "status"));
                    enrichStatus(paymentMap, paymentStatus);
                    if (firstStatus == null) {
                        firstStatus = paymentStatus;
                    }
                }
            }
            enrichStatus(result, firstStatus == null ? AdapayStatus.UNKNOWN : firstStatus);
            return;
        }
        AdapayStatus status = defaultStatus == null ? AdapayStatus.payment(getString(result, "status")) : defaultStatus;
        enrichStatus(result, status);
    }

    private void enrichRefundStatus(Map<String, Object> result) {
        if (result == null) {
            return;
        }
        Object refunds = result.get("refunds");
        if (refunds instanceof List) {
            AdapayStatus firstStatus = null;
            for (Object refund : (List) refunds) {
                if (refund instanceof Map) {
                    Map<String, Object> refundMap = (Map<String, Object>) refund;
                    AdapayStatus refundStatus = AdapayStatus.refund(firstNotEmpty(getString(refundMap, "trans_status"), getString(refundMap, "status")));
                    enrichStatus(refundMap, refundStatus);
                    if (firstStatus == null) {
                        firstStatus = refundStatus;
                    }
                }
            }
            enrichStatus(result, firstStatus == null ? AdapayStatus.UNKNOWN : firstStatus);
            return;
        }
        AdapayStatus status = AdapayStatus.refund(firstNotEmpty(getString(result, "status"), getString(result, "trans_status")));
        enrichStatus(result, status);
    }

    private void enrichStatus(Map<String, Object> result, AdapayStatus status) {
        if (result == null || status == null) {
            return;
        }
        result.put("sdk_status", status.getCode());
        result.put("sdk_status_desc", status.getDescription());
    }

    private <T> T executeWithConfig(AdapayInvoker<T> invoker, String errorMessage) {
        synchronized (AdapayPayConfigStorage.class) {
            try {
                payConfigStorage.initAdapayConfig();
                return invoker.invoke();
            } catch (PayErrorException e) {
                throw e;
            } catch (BaseAdaPayException e) {
                throw new PayErrorException(new PayException("-1", errorMessage + ": " + e.getMessage()));
            } catch (Exception e) {
                throw new PayErrorException(new PayException("-1", errorMessage + ": " + e.getMessage()));
            }
        }
    }

    private static String getString(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }

    private static String firstNotEmpty(String first, String second) {
        return StringUtils.isNotEmpty(first) ? first : second;
    }

    private interface AdapayInvoker<T> {
        T invoke() throws Exception;
    }
}
