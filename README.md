# pay-java-adapay

基于 Adapay 官方 Java SDK 实现的支付集成模块。

## 功能特性

- 支持多种支付渠道：
  - 支付宝（扫码、WAP、APP、PC 网页、小程序）
  - 微信（扫码、公众号、WAP、APP、小程序）
  - 银联（扫码、WAP、APP）
  - 收银台统一支付
  - 快捷支付
- 订单查询、关闭
- 退款及退款查询
- 账单下载

## 使用方法

### 1. 添加依赖

在 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.holuntech</groupId>
    <artifactId>pay-java-adapay</artifactId>
    <version>${version}</version>
</dependency>
```

### 2. 配置初始化

```java
// 创建配置
AdapayPayConfigStorage config = new AdapayPayConfigStorage();

// 设置 API Key（从 Adapay 控制台获取）
config.setApiKey("your_api_key");
config.setApiMockKey("your_api_mock_key");

// 设置 RSA 私钥（从 Adapay 控制台下载配置文件获取）
config.setRsaPrivateKey("your_rsa_private_key");

// 设置应用 ID
config.setAppId("your_app_id");

// 设置回调地址
config.setNotifyUrl("https://your-domain.com/notify");
config.setReturnUrl("https://your-domain.com/return");

// 设置环境（true=生产，false=测试）
config.setProdMode(false);
config.setDebug(true);

// 创建支付服务
AdapayPayService payService = new AdapayPayService(config);
```

### 3. 发起支付

```java
// 创建订单
PayOrder order = new PayOrder();
order.setSubject("商品标题");
order.setBody("商品描述");
order.setPrice(new BigDecimal("0.01"));
order.setOutTradeNo("ORDER_" + System.currentTimeMillis());
order.setTransactionType(AdapayTransactionType.ALIPAY_QR);

// 发起支付
Map<String, Object> result = payService.orderInfo(order);
String qrCode = (String) result.get("qr_code"); // 二维码地址
```

### 4. 收银台支付

```java
PayOrder order = new PayOrder();
order.setSubject("商品标题");
order.setBody("商品描述");
order.setPrice(new BigDecimal("0.01"));
order.setOutTradeNo("ORDER_" + System.currentTimeMillis());

// 使用收银台支付
Map<String, Object> result = payService.checkoutPay(order);
String payUrl = (String) result.get("pay_url"); // 收银台 URL
```

### 5. 订单查询

```java
Map<String, Object> result = payService.query(paymentId, null);
String status = (String) result.get("status");
```

### 6. 退款

```java
RefundOrder refundOrder = new RefundOrder();
refundOrder.setTradeNo(paymentId);
refundOrder.setRefundNo("REFUND_" + System.currentTimeMillis());
refundOrder.setRefundAmount(new BigDecimal("0.01"));
refundOrder.setDescription("退款原因");

RefundResult refundResult = payService.refund(refundOrder);
```

## 支付渠道列表

| 枚举值 | 说明 |
|-------|------|
| ALIPAY_QR | 支付宝扫码支付 |
| ALIPAY_WAP | 支付宝 WAP 支付 |
| ALIPAY_APP | 支付宝 APP 支付 |
| ALIPAY_PAGE | 支付宝 PC 网页支付 |
| ALIPAY_LITE | 支付宝小程序支付 |
| WX_PUB_QR | 微信扫码支付 |
| WX_PUB | 微信公众号支付 |
| WX_WAP | 微信 WAP 支付 |
| WX_APP | 微信 APP 支付 |
| WX_LITE | 微信小程序支付 |
| UNION_QR | 银联扫码支付 |
| UNION_WAP | 银联 WAP 支付 |
| UNION_APP | 银联 APP 支付 |
| CHECKOUT | 收银台支付 |
| FAST_PAY | 快捷支付 |

## 参考文档

- [Adapay 官方文档](https://docs.adapay.tech/api/index.html)
- [Adapay Java SDK 文档](https://docs.adapay.tech/sdk/javasdkaccess.html)
- [Adapay 控制台](https://console.adapay.tech/)

## 注意事项

1. 使用前需要先在 [Adapay 控制台](https://console.adapay.tech/) 注册并完成商户认证。
2. 从控制台获取 API Key 并下载 RSA 密钥配置文件。
3. 上传 RSA 公钥到控制台。
4. 生产环境请设置 `setProdMode(true)` 并关闭调试模式。
5. 异步通知需要配置公网可访问的 `notify_url`。

## 构建

```bash
mvn clean install -DskipTests
```
