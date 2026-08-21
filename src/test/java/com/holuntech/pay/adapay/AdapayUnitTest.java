package com.holuntech.pay.adapay;

import com.alibaba.fastjson.JSON;
import com.egzosn.pay.common.bean.RefundOrder;
import com.egzosn.pay.common.bean.TransactionType;
import com.egzosn.pay.common.exception.PayErrorException;
import com.egzosn.pay.common.util.DateUtils;
import com.holuntech.pay.adapay.api.AdapayPayConfigStorage;
import com.holuntech.pay.adapay.api.AdapayPayService;
import com.holuntech.pay.adapay.bean.AdapayDivMember;
import com.holuntech.pay.adapay.bean.AdapayPayMessage;
import com.holuntech.pay.adapay.bean.AdapayProfitSharingResult;
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
import java.util.Date;
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
    public void mapProfitSharingMethodsUsePaymentConfirmWrappers() {
        TestableAdapayPayService service = new TestableAdapayPayService(baseConfig(null));

        Map<String, Object> confirmParams = new HashMap<String, Object>();
        confirmParams.put("payment_id", "pay_1");
        confirmParams.put("order_no", "CONFIRM_1");
        confirmParams.put("confirm_amt", "0.10");

        Map<String, Object> confirmResult = service.profitSharingConfirm(confirmParams);

        assertEquals("app_test", service.paymentConfirmParams.get("app_id"));
        assertEquals("pay_1", service.paymentConfirmParams.get("payment_id"));
        assertEquals("pc_1", confirmResult.get("id"));
        assertEquals(AdapayStatus.SHARE_SUCCESS.getCode(), confirmResult.get("sdk_status"));

        Map<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put("payment_confirm_id", "pc_1");
        Map<String, Object> queryResult = service.profitSharingQuery(queryParams);

        assertEquals("pc_1", service.paymentConfirmQueryParams.get("payment_confirm_id"));
        assertEquals("pc_1", queryResult.get("id"));
        assertEquals(AdapayStatus.SHARE_PROCESSING.getCode(), queryResult.get("sdk_status"));

        Map<String, Object> listParams = new HashMap<String, Object>();
        listParams.put("payment_id", "pay_1");
        Map<String, Object> listResult = service.profitSharingQueryList(listParams);

        assertEquals("app_test", service.paymentConfirmQueryListParams.get("app_id"));
        assertEquals("pay_1", service.paymentConfirmQueryListParams.get("payment_id"));
        assertNotNull(listResult.get("payment_confirmations"));
    }

    @Test
    public void profitSharingConfirmKeepsDivMembersAsJsonArray() {
        TestableAdapayPayService service = new TestableAdapayPayService(baseConfig(null));
        List<AdapayDivMember> divMembers = new ArrayList<AdapayDivMember>();
        divMembers.add(new AdapayDivMember("member_1", new BigDecimal("0.02"), "Y"));

        AdapayProfitSharingResult result = service.profitSharingConfirm(
                "pay_1", "CONFIRM_1", new BigDecimal("0.03"), divMembers);

        Object value = service.paymentConfirmParams.get("div_members");
        assertTrue(value instanceof List);
        assertEquals("member_1", ((Map<?, ?>) ((List<?>) value).get(0)).get("member_id"));
        assertTrue(JSON.parseObject(JSON.toJSONString(service.paymentConfirmParams))
                .get("div_members") instanceof List);
        assertTrue(result.isSuccess());
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

    @Test
    public void querySettleDetailBuildsSdkRequestParameters() {
        TestableAdapayPayService service = new TestableAdapayPayService(baseConfig(null));
        Date beginDate = DateUtils.parseDate("2024-01-01", DateUtils.YYYY_MM_DD);
        Date endDate = DateUtils.parseDate("2024-01-31", DateUtils.YYYY_MM_DD);

        Map<String, Object> result = service.querySettleDetail("member_1", "settle_1", beginDate, endDate);

        assertEquals("app_test", service.settleDetailParams.get("app_id"));
        assertEquals("member_1", service.settleDetailParams.get("member_id"));
        assertEquals("settle_1", service.settleDetailParams.get("settle_account_id"));
        assertEquals("20240101", service.settleDetailParams.get("begin_date"));
        assertEquals("20240131", service.settleDetailParams.get("end_date"));
        assertEquals("list", result.get("object"));
        assertEquals("succeeded", result.get("status"));

        List<Map<String, Object>> details = (List<Map<String, Object>>) result.get("settle_details");
        assertEquals(1, details.size());
        assertEquals("20191014", details.get(0).get("settle_date"));
        assertEquals("6.98", details.get(0).get("settle_amt"));
        assertEquals("0.00", details.get(0).get("settle_fee_amt"));
        assertEquals("succeeded", details.get(0).get("settle_stat"));
        assertEquals("T1", details.get(0).get("settle_type"));
        assertEquals("", details.get(0).get("settle_message"));
    }

    @Test
    public void querySettleDetailForMerchantMayOmitSettleAccount() {
        TestableAdapayPayService service = new TestableAdapayPayService(baseConfig(null));
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("member_id", "0");
        params.put("begin_date", "2024-01-01");
        params.put("end_date", "20240102");

        service.querySettleDetail(params);

        assertEquals("0", service.settleDetailParams.get("member_id"));
        assertFalse(service.settleDetailParams.containsKey("settle_account_id"));
        assertEquals("20240101", service.settleDetailParams.get("begin_date"));
        assertEquals("20240102", service.settleDetailParams.get("end_date"));
    }

    @Test
    public void querySettleDetailRejectsDateRangeOver31Days() {
        TestableAdapayPayService service = new TestableAdapayPayService(baseConfig(null));

        try {
            service.querySettleDetail("member_1", "settle_1",
                    DateUtils.parseDate("2024-01-01", DateUtils.YYYY_MM_DD),
                    DateUtils.parseDate("2024-02-02", DateUtils.YYYY_MM_DD));
            fail("expected PayErrorException for date range over 31 days");
        } catch (PayErrorException e) {
            assertTrue(e.getMessage().contains("31"));
        }
    }

    @Test
    public void querySettleDetailReturnsEmptyDetailsWithoutError() {
        TestableAdapayPayService service = new TestableAdapayPayService(baseConfig(null));
        Map<String, Object> emptyResult = new HashMap<String, Object>();
        emptyResult.put("object", "list");
        emptyResult.put("status", "succeeded");
        emptyResult.put("settle_details", new ArrayList<Map<String, Object>>());
        service.settleDetailResult = emptyResult;

        Map<String, Object> result = service.querySettleDetail("member_1", null,
                DateUtils.parseDate("2024-01-01", DateUtils.YYYY_MM_DD),
                DateUtils.parseDate("2024-01-01", DateUtils.YYYY_MM_DD));

        assertEquals("succeeded", result.get("status"));
        assertTrue(((List<?>) result.get("settle_details")).isEmpty());
    }

    @Test
    public void querySettleDetailPreservesAdaPayFailureCodeAndMessage() {
        TestableAdapayPayService service = new TestableAdapayPayService(baseConfig(null));
        Map<String, Object> failureResult = new HashMap<String, Object>();
        failureResult.put("object", "list");
        failureResult.put("status", "failed");
        failureResult.put("error_code", "invalid_param");
        failureResult.put("error_msg", "member_id is invalid");
        service.settleDetailResult = failureResult;

        Map<String, Object> result = service.querySettleDetail("member_1", null,
                DateUtils.parseDate("2024-01-01", DateUtils.YYYY_MM_DD),
                DateUtils.parseDate("2024-01-01", DateUtils.YYYY_MM_DD));

        assertEquals("failed", result.get("status"));
        assertEquals("invalid_param", result.get("error_code"));
        assertEquals("member_id is invalid", result.get("error_msg"));
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
        private Map<String, Object> paymentConfirmParams;
        private Map<String, Object> paymentConfirmQueryParams;
        private Map<String, Object> paymentConfirmQueryListParams;
        private Map<String, Object> settleDetailParams;
        private Map<String, Object> settleDetailResult = defaultSettleDetailResult();

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
        protected Map<String, Object> createPaymentConfirm(Map<String, Object> params) {
            paymentConfirmParams = new HashMap<String, Object>(params);
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("id", "pc_1");
            result.put("payment_id", params.get("payment_id"));
            result.put("order_no", params.get("order_no"));
            result.put("confirm_amt", params.get("confirm_amt"));
            result.put("status", "succeeded");
            return result;
        }

        @Override
        protected Map<String, Object> queryPaymentConfirm(Map<String, Object> params) {
            paymentConfirmQueryParams = new HashMap<String, Object>(params);
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("id", params.get("payment_confirm_id"));
            result.put("status", "pending");
            return result;
        }

        @Override
        protected Map<String, Object> queryPaymentConfirmList(Map<String, Object> params) {
            paymentConfirmQueryListParams = new HashMap<String, Object>(params);

            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", "pc_1");
            item.put("payment_id", params.get("payment_id"));
            item.put("status", "succeeded");

            List<Map<String, Object>> confirmations = new ArrayList<Map<String, Object>>();
            confirmations.add(item);

            Map<String, Object> result = new HashMap<String, Object>();
            result.put("payment_confirmations", confirmations);
            return result;
        }

        @Override
        protected Map<String, Object> querySettleDetails(Map<String, Object> params) {
            settleDetailParams = new HashMap<String, Object>(params);
            return settleDetailResult;
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

        private static Map<String, Object> defaultSettleDetailResult() {
            Map<String, Object> detail = new HashMap<String, Object>();
            detail.put("card_name", "测试商户");
            detail.put("card_no", "130234****8399");
            detail.put("settle_date", "20191014");
            detail.put("settle_amt", "6.98");
            detail.put("settle_fee_amt", "0.00");
            detail.put("settle_stat", "succeeded");
            detail.put("settle_type", "T1");
            detail.put("settle_message", "");

            List<Map<String, Object>> details = new ArrayList<Map<String, Object>>();
            details.add(detail);
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("object", "list");
            result.put("prod_mode", "true");
            result.put("status", "succeeded");
            result.put("settle_details", details);
            return result;
        }
    }
}
