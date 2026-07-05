package com.holuntech.pay.adapay;

import com.holuntech.pay.adapay.api.AdapayPayConfigStorage;
import com.holuntech.pay.adapay.api.AdapayPayService;
import com.holuntech.pay.adapay.bean.AdapayTransactionType;
import com.egzosn.pay.common.bean.PayOrder;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Adapay支付测试
 *
 * @author egan
 */
public class PayTest {

    private AdapayPayService payService;

    @Before
    public void setUp() {
        // 创建配置
        AdapayPayConfigStorage config = new AdapayPayConfigStorage();
        
        // 设置API Key（需要从Adapay控制台获取）
        config.setApiKey("your_api_key_here");
        config.setApiMockKey("your_api_mock_key_here");
        
        // 设置RSA私钥（需要从Adapay控制台下载配置文件获取）
        config.setRsaPrivateKey("your_rsa_private_key_here");
        
        // 设置应用ID
        config.setAppId("your_app_id_here");
        
        // 设置回调地址
        config.setNotifyUrl("https://your-domain.com/pay/adapay/notify");
        config.setReturnUrl("https://your-domain.com/pay/adapay/return");
        
        // 设置为测试模式
        config.setProdMode(false);
        config.setDebug(true);
        
        // 创建支付服务
        payService = new AdapayPayService(config);
    }

    /**
     * 测试支付宝扫码支付
     */
    @Test
    public void testAlipayQrPay() {
        PayOrder order = new PayOrder();
        order.setSubject("测试商品");
        order.setBody("测试商品描述");
        order.setPrice(new BigDecimal("0.01"));
        order.setOutTradeNo("TEST_ORDER_" + System.currentTimeMillis());
        order.setTransactionType(AdapayTransactionType.ALIPAY_QR);
        
        try {
            Map<String, Object> result = payService.orderInfo(order);
            System.out.println("支付结果: " + result);
            System.out.println("支付ID: " + result.get("id"));
            System.out.println("二维码地址: " + result.get("qr_code"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试微信公众号支付
     */
    @Test
    public void testWxPubPay() {
        PayOrder order = new PayOrder();
        order.setSubject("测试商品");
        order.setBody("测试商品描述");
        order.setPrice(new BigDecimal("0.01"));
        order.setOutTradeNo("TEST_ORDER_" + System.currentTimeMillis());
        order.setTransactionType(AdapayTransactionType.WX_PUB);
        
        try {
            Map<String, Object> result = payService.orderInfo(order);
            System.out.println("支付结果: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试收银台支付
     */
    @Test
    public void testCheckoutPay() {
        PayOrder order = new PayOrder();
        order.setSubject("测试商品");
        order.setBody("测试商品描述");
        order.setPrice(new BigDecimal("0.01"));
        order.setOutTradeNo("TEST_ORDER_" + System.currentTimeMillis());
        order.setTransactionType(AdapayTransactionType.CHECKOUT);
        
        try {
            Map<String, Object> result = payService.checkoutPay(order);
            System.out.println("支付结果: " + result);
            System.out.println("收银台地址: " + result.get("pay_url"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试订单查询
     */
    @Test
    public void testQuery() {
        String paymentId = "your_payment_id_here";
        
        try {
            Map<String, Object> result = payService.query(paymentId, null);
            System.out.println("查询结果: " + result);
            System.out.println("订单状态: " + result.get("status"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试关闭订单
     */
    @Test
    public void testClose() {
        String paymentId = "your_payment_id_here";
        
        try {
            Map<String, Object> result = payService.close(paymentId, null);
            System.out.println("关闭结果: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
