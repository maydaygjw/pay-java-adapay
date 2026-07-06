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
- **分账（实时分账、延时分账、分账确认、分账撤销、分账查询）**

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

> 本模块基于上游 `com.egzosn:pay-java-common` 实现，因此 `PayOrder`、`RefundOrder` 等公共类来自 `com.egzosn.pay.common.*`，而 Adapay 相关类来自 `com.holuntech.pay.adapay.*`。

### 2. 配置初始化

```java
import com.holuntech.pay.adapay.api.AdapayPayConfigStorage;
import com.holuntech.pay.adapay.api.AdapayPayService;

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
import com.egzosn.pay.common.bean.PayOrder;
import com.holuntech.pay.adapay.api.AdapayPayService;
import com.holuntech.pay.adapay.bean.AdapayTransactionType;

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
import com.egzosn.pay.common.bean.PayOrder;
import com.holuntech.pay.adapay.api.AdapayPayService;

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
import com.egzosn.pay.common.bean.RefundOrder;
import com.egzosn.pay.common.bean.RefundResult;
import com.holuntech.pay.adapay.api.AdapayPayService;

RefundOrder refundOrder = new RefundOrder();
refundOrder.setTradeNo(paymentId);
refundOrder.setRefundNo("REFUND_" + System.currentTimeMillis());
refundOrder.setRefundAmount(new BigDecimal("0.01"));
refundOrder.setDescription("退款原因");

RefundResult refundResult = payService.refund(refundOrder);
```

### 7. 分账

#### 7.1 实时分账

支付时直接传入 `div_members`，支付成功后会自动分账。

```java
import com.holuntech.pay.adapay.bean.AdapayDivMember;

List<AdapayDivMember> divMembers = new ArrayList<>();
divMembers.add(new AdapayDivMember("member_id_001", new BigDecimal("0.05"), "Y")); // 手续费承担方
divMembers.add(new AdapayDivMember("member_id_002", new BigDecimal("0.03"), "N"));

Map<String, Object> result = payService.orderInfoWithProfitSharing(order, divMembers);
```

#### 7.2 延时分账

先发起延时支付，支付完成后再调用分账确认。

```java
// 1. 发起延时分账支付
Map<String, Object> delayResult = payService.orderInfoWithDelayProfitSharing(order);
String paymentId = (String) delayResult.get("id");

// 2. 延时分账确认
AdapayProfitSharingResult confirmResult = payService.profitSharingConfirm(
        paymentId,
        "CONFIRM_" + System.currentTimeMillis(),
        new BigDecimal("0.08"),
        divMembers,
        "分账确认",
        "I" // I-交易金额中扣取手续费，O-商户手续费账户扣取
);
```

#### 7.3 分账撤销

仅对已支付完成、未确认成功的延时分账订单可撤销。

```java
AdapayReverseResult reverseResult = payService.profitSharingReverse(
        paymentId,
        "REVERSE_" + System.currentTimeMillis(),
        new BigDecimal("0.08"),
        "撤销原因"
);
```

#### 7.4 分账查询

```java
// 查询分账确认单
Map<String, Object> confirmDetail = payService.queryProfitSharingConfirm(paymentConfirmId);

// 查询分账确认单列表
Map<String, Object> confirmList = payService.queryProfitSharingConfirmList(paymentId, null, 1, 10);

// 查询分账撤销单
Map<String, Object> reverseDetail = payService.queryProfitSharingReverse(reverseId);

// 查询分账撤销单列表
Map<String, Object> reverseList = payService.queryProfitSharingReverseList(paymentId, 1, 10);
```

#### 7.5 分账对象与结算账户

```java
// 创建个人分账对象（用于分账时不要上传手机号、姓名、证件信息）
Map<String, Object> memberParams = new HashMap<>();
memberParams.put("member_id", "member_id_001");
Map<String, Object> memberResult = payService.createDivMember(memberParams);

// 为企业分账对象开户
Map<String, Object> corpParams = new HashMap<>();
corpParams.put("member_id", "member_id_corp_001");
corpParams.put("order_no", "CORP_" + System.currentTimeMillis());
// ... 其他企业必填字段
Map<String, Object> corpResult = payService.createCorpDivMember(corpParams, new File("/path/to/attach.zip"));

// 为分账对象绑定结算银行卡
Map<String, Object> settleParams = new HashMap<>();
settleParams.put("member_id", "member_id_001");
settleParams.put("channel", "channel_code");
Map<String, Object> accountInfo = new HashMap<>();
accountInfo.put("card_id", "622202...");
accountInfo.put("card_name", "张三");
// ... 其他 account_info 字段
settleParams.put("account_info", accountInfo);
Map<String, Object> settleResult = payService.createDivSettleAccount(settleParams);
```

## yshop / Spring Boot 接入

模块同时提供 Spring Boot 自动配置：

- Spring Boot 2：`META-INF/spring.factories`
- Spring Boot 3：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

yshop 的 `merchant_details.pay_type` 建议配置为：

```text
adapay
```

自动配置类会注册 `AdapayPaymentPlatform`，平台名为 `adapay`，可被 `pay-spring-boot-starter` 收集并通过 `PaymentPlatforms.getPaymentPlatform("adapay")` 找到。

### merchant_details 字段映射

| yshop / 通用字段 | Adapay 配置 |
|------------------|-------------|
| appid / app_id | appId |
| pid / api_key | apiKey |
| api_mock_key / api_key_test | apiMockKey |
| key_private / private_key | rsaPrivateKey |
| key_public / rsa_public_key | rsaPublicKey |
| seller / merchant_key / merchantKey | merchantKey |
| is_test | prodMode = !is_test |
| notify_url | notifyUrl |
| return_url | returnUrl |

多商户场景建议每个租户配置独立 `merchantKey`。未显式配置时 SDK 会基于 `appId/apiKey/apiMockKey` 生成隔离 key，但显式保存更利于排查。

### 回调验签要求

Adapay 异步通知验签必须使用回调表单里的原始 `data` 字符串和 `sign`：

```java
boolean ok = payService.verifyRawData(rawData, sign);
```

不要把 `data` 解析成 `Map` 后再验签，`Map.toString()` 与 Adapay 原始签名串不同，会导致验签失败。通过 `payBack(...)` 走默认表单解析时，`data` 会保持字符串；如果业务 Web 层提前解析了请求，请额外保留原始 `data`。

### transactionType 推荐值

Spring 平台适配同时支持枚举名和 Adapay code：

```text
WX_LITE
wx_lite
ALIPAY_WAP
alipay_wap
```

yshop 字符串配置推荐直接使用 Adapay code，例如 `wx_lite`、`alipay_wap`、`alipay_qr`、`wx_pub_qr`、`checkout`。

### 查询与退款订单号

查询支持：

- `payment_id`：`query(paymentId, null)`
- `order_no`：`query(null, orderNo)`

退款接口仍以 Adapay `payment_id` 为准。如果 `RefundOrder.tradeNo` 为空但提供了 `outTradeNo/order_no`，SDK 会先按 `order_no` 查询 `payment_id` 再退款。生产链路仍建议保存 Adapay 返回的 `payment_id`，可减少一次查询并避免同一商户订单号异常重复时产生歧义。

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
