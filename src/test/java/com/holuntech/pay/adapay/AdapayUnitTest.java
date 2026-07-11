package com.holuntech.pay.adapay;

import com.alibaba.fastjson.JSON;
import com.egzosn.pay.common.bean.RefundOrder;
import com.egzosn.pay.common.bean.TransactionType;
import com.egzosn.pay.common.exception.PayErrorException;
import com.holuntech.pay.adapay.api.AdapayPayConfigStorage;
import com.holuntech.pay.adapay.api.AdapayPayService;
import com.holuntech.pay.adapay.bean.AdapayPayMessage;
import com.holuntech.pay.adapay.bean.AdapayRefundResult;
import com.holuntech.pay.adapay.bean.AdapayReverseResult;
import com.holuntech.pay.adapay.bean.AdapayStatus;
import com.holuntech.pay.adapay.bean.AdapayTransactionType;
import com.holuntech.pay.adapay.spring.boot.core.provider.merchant.platform.AdapayPaymentPlatform;
import com.huifu.adapay.core.util.AdapaySign;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AdapayUnitTest {

    @Test
    public void springLoadsAdapayPaymentPlatform() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(AdapayPaymentPlatform.class);
        context.refresh();

        assertNotNull(context.getBean(AdapayPaymentPlatform.class));
        assertEquals(AdapayPaymentPlatform.PLATFORM_NAME, context.getBean(AdapayPaymentPlatform.class).getPlatform());
        context.close();
    }

    @Test
    public void transactionTypeSupportsEnumNameAndCode() {
        AdapayPaymentPlatform platform = new AdapayPaymentPlatform();

        TransactionType enumName = platform.getTransactionType("WX_LITE");
        TransactionType code = platform.getTransactionType("wx_lite");

        assertSame(AdapayTransactionType.WX_LITE, enumName);
        assertSame(AdapayTransactionType.WX_LITE, code);
        assertSame(AdapayTransactionType.ALIPAY_WAP, platform.getTransactionType("alipay_wap"));
    }

    @Test
    public void callbackSignVerifyRequiresRawData() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair keyPair = generator.generateKeyPair();

        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String data = "{\"id\":\"pay_1\",\"order_no\":\"ORDER_1\",\"status\":\"succeeded\"}";
        String sign = AdapaySign.sign(data, privateKey);

        AdapayPayService service = new AdapayPayService(baseConfig(publicKey));
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("data", data);
        params.put("sign", sign);

        assertTrue(service.verify(params));
        params.put("sign", sign.substring(1));
        assertFalse(service.verify(params));

        params.put("sign", sign);
        params.put("data", JSON.parseObject(data));
        assertFalse(service.verify(params));
        assertTrue(service.verifyRawData(data, sign));
    }

    @Test
    public void payMessageParsesPaymentAndRefundNotification() {
        Map<String, Object> paymentEvent = new HashMap<String, Object>();
        paymentEvent.put("id", "evt_1");
        paymentEvent.put("type", "payment.succeeded");
        paymentEvent.put("object", "payment");
        paymentEvent.put("data", "{\"id\":\"pay_1\",\"order_no\":\"ORDER_1\",\"pay_amt\":\"0.10\",\"status\":\"succeeded\"}");

        AdapayPayMessage paymentMessage = new AdapayPayMessage(paymentEvent);
        assertEquals("ORDER_1", paymentMessage.getOutTradeNo());
        assertEquals("pay_1", paymentMessage.getPaymentId());
        assertEquals("succeeded", paymentMessage.getStatus());
        assertEquals(AdapayStatus.PAY_SUCCESS, paymentMessage.getAdapayStatus());
        assertEquals(new BigDecimal("0.10"), paymentMessage.getTotalFee());

        Map<String, Object> refundEvent = new HashMap<String, Object>();
        refundEvent.put("id", "evt_2");
        refundEvent.put("type", "refund.failed");
        refundEvent.put("object", "refund");
        refundEvent.put("data", "{\"id\":\"refund_1\",\"payment_id\":\"pay_1\",\"refund_order_no\":\"REFUND_1\",\"refund_amt\":\"0.05\",\"status\":\"failed\"}");

        AdapayPayMessage refundMessage = new AdapayPayMessage(refundEvent);
        assertEquals("pay_1", refundMessage.getPaymentId());
        assertEquals("refund_1", refundMessage.getRefundId());
        assertEquals("REFUND_1", refundMessage.getRefundOrderNo());
        assertEquals(AdapayStatus.REFUND_FAILED, refundMessage.getAdapayStatus());
    }

    @Test
    public void refundResolvesPaymentIdFromOrderNo() {
        TestableAdapayPayService service = new TestableAdapayPayService(baseConfig(null));
        RefundOrder refundOrder = new RefundOrder();
        refundOrder.setOutTradeNo("ORDER_1");
        refundOrder.setRefundNo("REFUND_1");
        refundOrder.setRefundAmount(new BigDecimal("0.01"));

        AdapayRefundResult result = service.refund(refundOrder);

        assertEquals("pay_from_order", service.refundPaymentId);
        assertEquals("pay_from_order", result.getPaymentId());
        assertTrue(result.isProcessing());
    }

    @Test
    public void reverseResultStatusHelpers() {
        AdapayReverseResult result = new AdapayReverseResult();

        result.setStatus("succeeded");
        assertTrue(result.isSuccess());
        assertFalse(result.isProcessing());
        assertFalse(result.isFailed());

        result.setStatus("pending");
        assertTrue(result.isProcessing());

        result.setStatus("failed");
        assertTrue(result.isFailed());
    }

    @Test
    public void deleteProfitRecipientDeletesSettleAccount() {
        TestableAdapayPayService service = new TestableAdapayPayService(baseConfig(null));

        Map<String, Object> result = service.deleteProfitRecipient("settle_1");

        assertEquals("settle_1", service.deletedSettleAccountParams.get("settle_account_id"));
        assertEquals("app_test", service.deletedSettleAccountParams.get("app_id"));
        assertEquals("settle_1", result.get("settle_account_id"));
        assertEquals("succeeded", result.get("status"));
    }

    @Test
    public void reverseValidatesInput() {
        TestableAdapayPayService service = new TestableAdapayPayService(baseConfig(null));

        try {
            service.reverse(null, "ORDER_REVERSE_1", new BigDecimal("0.01"));
            fail("expected PayErrorException for empty paymentId");
        } catch (PayErrorException e) {
            assertTrue(e.getMessage().contains("payment_id"));
        }

        try {
            service.reverse("pay_1", null, new BigDecimal("0.01"));
            fail("expected PayErrorException for empty orderNo");
        } catch (PayErrorException e) {
            assertTrue(e.getMessage().contains("order_no"));
        }

        try {
            service.reverse("pay_1", "ORDER_REVERSE_1", BigDecimal.ZERO);
            fail("expected PayErrorException for zero reverseAmt");
        } catch (PayErrorException e) {
            assertTrue(e.getMessage().contains("reverse_amt"));
        }
    }

    @Test
    public void reverseCreatesRequestAndReturnsResult() {
        AdapayPayConfigStorage config = baseConfig(null);
        config.setNotifyUrl("https://example.com/notify");
        TestableAdapayPayService service = new TestableAdapayPayService(config);

        AdapayReverseResult result = service.reverse("pay_1", "ORDER_REVERSE_1", new BigDecimal("1.23"), "test reason");

        Map<String, Object> params = service.reverseParams;
        assertEquals("app_test", params.get("app_id"));
        assertEquals("pay_1", params.get("payment_id"));
        assertEquals("ORDER_REVERSE_1", params.get("order_no"));
        assertEquals("1.23", params.get("reverse_amt"));
        assertEquals("test reason", params.get("reason"));
        assertEquals("https://example.com/notify", params.get("notify_url"));

        assertEquals("reverse_1", result.getId());
        assertEquals("pay_1", result.getPaymentId());
        assertEquals("ORDER_REVERSE_1", result.getOrderNo());
        assertEquals(new BigDecimal("1.23"), result.getReverseAmt());
        assertEquals(new BigDecimal("1.23"), result.getReversedAmt());
        assertEquals(new BigDecimal("0.00"), result.getConfirmedAmt());
        assertEquals(new BigDecimal("0.00"), result.getRefundedAmt());
        assertTrue(result.isSuccess());
    }

    @Test
    public void reverseWithoutReasonAndNotifyUrl() {
        AdapayPayConfigStorage config = baseConfig(null);
        TestableAdapayPayService service = new TestableAdapayPayService(config);

        service.reverse("pay_1", "ORDER_REVERSE_2", new BigDecimal("0.01"));

        Map<String, Object> params = service.reverseParams;
        assertNull(params.get("reason"));
        assertNull(params.get("notify_url"));
    }

    @Test
    public void profitSharingReverseStillWorks() {
        AdapayPayConfigStorage config = baseConfig(null);
        config.setNotifyUrl("https://example.com/notify");
        TestableAdapayPayService service = new TestableAdapayPayService(config);

        AdapayReverseResult result = service.profitSharingReverse("pay_1", "ORDER_REVERSE_3", new BigDecimal("0.02"), "reason", "https://custom/notify");

        Map<String, Object> params = service.reverseParams;
        assertEquals("https://custom/notify", params.get("notify_url"));
        assertEquals("pay_1", result.getPaymentId());
        assertTrue(result.isSuccess());
    }

    private static AdapayPayConfigStorage baseConfig(String publicKey) {
        AdapayPayConfigStorage config = new AdapayPayConfigStorage();
        config.setAppId("app_test");
        config.setApiKey("api_key");
        config.setApiMockKey("mock_key");
        config.setRsaPublicKey(publicKey);
        config.setRsaPrivateKey("private_key");
        config.setProdMode(false);
        config.setMerchantKey("merchant_test");
        return config;
    }

    private static class TestableAdapayPayService extends AdapayPayService {

        private String refundPaymentId;
        private Map<String, Object> deletedSettleAccountParams;
        private Map<String, Object> reverseParams;

        TestableAdapayPayService(AdapayPayConfigStorage payConfigStorage) {
            super(payConfigStorage);
        }

        @Override
        protected Map<String, Object> queryPaymentList(Map<String, Object> params) {
            Map<String, Object> payment = new HashMap<String, Object>();
            payment.put("id", "pay_from_order");
            payment.put("order_no", params.get("order_no"));
            payment.put("status", "succeeded");

            List<Map<String, Object>> payments = new ArrayList<Map<String, Object>>();
            payments.add(payment);

            Map<String, Object> result = new HashMap<String, Object>();
            result.put("payments", payments);
            result.put("status", "succeeded");
            return result;
        }

        @Override
        protected Map<String, Object> createRefund(String paymentId, Map<String, Object> params) {
            refundPaymentId = paymentId;
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("id", "refund_1");
            result.put("payment_id", paymentId);
            result.put("refund_order_no", params.get("refund_order_no"));
            result.put("refund_amt", params.get("refund_amt"));
            result.put("status", "pending");
            return result;
        }

        @Override
        protected Map<String, Object> deleteSettleAccount(Map<String, Object> params) {
            deletedSettleAccountParams = new HashMap<String, Object>(params);
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("settle_account_id", params.get("settle_account_id"));
            result.put("status", "succeeded");
            return result;
        }

        @Override
        protected Map<String, Object> createReverse(Map<String, Object> params) {
            reverseParams = params;
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("id", "reverse_1");
            result.put("payment_id", params.get("payment_id"));
            result.put("order_no", params.get("order_no"));
            result.put("reverse_amt", params.get("reverse_amt"));
            result.put("reversed_amt", params.get("reverse_amt"));
            result.put("confirmed_amt", "0.00");
            result.put("refunded_amt", "0.00");
            result.put("status", "succeeded");
            return result;
        }
    }
}
