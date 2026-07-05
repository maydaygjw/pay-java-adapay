package com.holuntech.pay.adapay.bean;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.egzosn.pay.common.bean.PayMessage;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapay异步通知消息。
 *
 * @author egan
 */
public class AdapayPayMessage extends PayMessage {

    private final Map<String, Object> data;

    public AdapayPayMessage(Map<String, Object> payMessage) {
        super(payMessage);
        this.data = parseData(payMessage == null ? null : payMessage.get("data"));
        setPayType("adapay");
        setTransactionType(getEventType());
        setFromPay(getObject());
        setDescribe(getStatus());
    }

    public String getEventId() {
        return getString(getPayMessage(), "id");
    }

    public String getEventType() {
        return getString(getPayMessage(), "type");
    }

    public String getObject() {
        String object = getString(data, "object");
        return object == null ? getString(getPayMessage(), "object") : object;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public String getPaymentId() {
        String paymentId = getString(data, "payment_id");
        if (paymentId != null) {
            return paymentId;
        }
        if ("payment".equalsIgnoreCase(getObject()) || isPaymentEvent()) {
            return getString(data, "id");
        }
        return getString(getPayMessage(), "payment_id");
    }

    public String getRefundId() {
        String refundId = getString(data, "refund_id");
        if (refundId != null) {
            return refundId;
        }
        if ("refund".equalsIgnoreCase(getObject()) || isRefundEvent()) {
            return getString(data, "id");
        }
        return null;
    }

    @Override
    public String getOutTradeNo() {
        String orderNo = getString(data, "order_no");
        return orderNo == null ? getString(getPayMessage(), "order_no") : orderNo;
    }

    public String getRefundOrderNo() {
        return getString(data, "refund_order_no");
    }

    public String getStatus() {
        String status = getString(data, "status");
        if (status == null) {
            status = getString(data, "trans_status");
        }
        return status == null ? getString(getPayMessage(), "status") : status;
    }

    public AdapayStatus getAdapayStatus() {
        return AdapayStatus.from(getEventType(), getStatus(), getObject());
    }

    @Override
    public Number getTotalFee() {
        String amount = getString(data, "pay_amt");
        if (amount == null) {
            amount = getString(data, "refund_amt");
        }
        if (amount == null || amount.length() == 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(amount);
    }

    private boolean isPaymentEvent() {
        String eventType = getEventType();
        return eventType != null && eventType.startsWith("payment.");
    }

    private boolean isRefundEvent() {
        String eventType = getEventType();
        return eventType != null && eventType.startsWith("refund.");
    }

    private static Map<String, Object> parseData(Object data) {
        if (data instanceof Map) {
            return new HashMap<String, Object>((Map<String, Object>) data);
        }
        if (data instanceof String && ((String) data).length() > 0) {
            JSONObject jsonObject = JSON.parseObject((String) data);
            return new HashMap<String, Object>(jsonObject);
        }
        return new HashMap<String, Object>();
    }

    private static String getString(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }
}
