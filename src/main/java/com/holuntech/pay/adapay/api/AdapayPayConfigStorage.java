package com.holuntech.pay.adapay.api;

import com.egzosn.pay.common.api.BasePayConfigStorage;
import com.huifu.adapay.Adapay;
import com.huifu.adapay.model.MerConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapay支付配置存储
 * 基于Adapay官方SDK实现
 *
 * @author egan
 */
public class AdapayPayConfigStorage extends BasePayConfigStorage {

    /**
     * API Key (生产环境)
     */
    private String apiKey;

    /**
     * API Key (测试环境/Mock模式)
     */
    private String apiMockKey;

    /**
     * RSA私钥 - 商户发起请求时用于参数加签
     */
    private String rsaPrivateKey;

    /**
     * RSA公钥 (可选，Adapay平台公钥，用于验证返回签名)
     */
    private String rsaPublicKey;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 是否为生产模式，默认true
     * true: 使用生产环境API
     * false: 使用Mock测试环境
     */
    private boolean prodMode = true;

    /**
     * 是否开启调试模式，默认false
     * 开启后会有详细的日志输出
     * 也可通过环境变量 ADAPAY_DEBUG=true 开启
     */
    private boolean debug = Boolean.parseBoolean(System.getenv().getOrDefault("ADAPAY_DEBUG", "false"));

    /**
     * 是否验证RSA签名，默认true
     */
    private boolean verifyRsa = true;

    /**
     * 设备ID (可选)
     * 单服务器时传入固定ID，集群服务器可忽略
     */
    private String deviceId = "Adapay_0001";

    /**
     * Adapay API地址
     */
    private String apiBase = "https://api.adapay.tech";

    /**
     * Adapay Page地址 (用于收银台等页面)
     */
    private String pageBase = "https://page.adapay.tech";

    /**
     * 商户配置Key (多商户模式使用)
     */
    private String merchantKey;

    /**
     * 未显式设置merchantKey时，为当前配置生成的隔离Key。
     */
    private volatile String generatedMerchantKey;

    /**
     * 初始化Adapay SDK配置
     * 在使用前必须调用此方法
     */
    public synchronized void initAdapayConfig() throws Exception {
        // 配置全局参数
        Adapay.debug = this.debug;
        Adapay.prodMode = this.prodMode;
        Adapay.verifyRSA = this.verifyRsa;
        Adapay.apiBase = this.apiBase;
        Adapay.pageBase = this.pageBase;
        Adapay.deviceID = this.deviceId;

        // Adapay SDK的公钥和环境字段是静态全局值，必须按当前租户覆盖。
        Adapay.publicKey = this.rsaPublicKey == null ? "" : this.rsaPublicKey;

        // 创建商户配置
        MerConfig merConfig = new MerConfig();
        merConfig.setApiKey(this.apiKey);
        merConfig.setApiMockKey(this.apiMockKey);
        merConfig.setRSAPrivateKey(this.rsaPrivateKey);
        merConfig.setRSAPublicKey(this.rsaPublicKey);
        merConfig.setDeviceId(this.deviceId);

        // 初始化SDK
        String effectiveMerchantKey = getMerchantKey();
        if (Adapay.defaultMerchantKey.equals(effectiveMerchantKey)) {
            Adapay.initWithMerConfig(merConfig);
        } else {
            Map<String, MerConfig> configs = new HashMap<String, MerConfig>(1);
            configs.put(effectiveMerchantKey, merConfig);
            Adapay.initWithMerConfigs(configs);
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public AdapayPayConfigStorage setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public String getApiMockKey() {
        return apiMockKey;
    }

    public AdapayPayConfigStorage setApiMockKey(String apiMockKey) {
        this.apiMockKey = apiMockKey;
        return this;
    }

    public String getRsaPrivateKey() {
        return rsaPrivateKey;
    }

    public AdapayPayConfigStorage setRsaPrivateKey(String rsaPrivateKey) {
        this.rsaPrivateKey = rsaPrivateKey;
        setKeyPrivate(rsaPrivateKey);
        return this;
    }

    @Override
    public void setKeyPrivate(String keyPrivate) {
        super.setKeyPrivate(keyPrivate);
        this.rsaPrivateKey = keyPrivate;
    }

    public String getRsaPublicKey() {
        return rsaPublicKey;
    }

    public AdapayPayConfigStorage setRsaPublicKey(String rsaPublicKey) {
        this.rsaPublicKey = rsaPublicKey;
        setKeyPublic(rsaPublicKey);
        return this;
    }

    @Override
    public void setKeyPublic(String keyPublic) {
        super.setKeyPublic(keyPublic);
        this.rsaPublicKey = keyPublic;
    }

    @Override
    public String getAppId() {
        return appId;
    }

    @Override
    public String getAppid() {
        return appId;
    }

    public AdapayPayConfigStorage setAppId(String appId) {
        this.appId = appId;
        return this;
    }

    public AdapayPayConfigStorage setAppid(String appId) {
        return setAppId(appId);
    }

    public String getSeller() {
        // Adapay没有seller概念，返回merchantKey
        return merchantKey;
    }

    public AdapayPayConfigStorage setSeller(String seller) {
        return setMerchantKey(seller);
    }

    @Override
    public String getPid() {
        // Adapay没有PID概念，返回apiKey
        return apiKey;
    }

    public boolean isProdMode() {
        return prodMode;
    }

    public AdapayPayConfigStorage setProdMode(boolean prodMode) {
        this.prodMode = prodMode;
        super.setTest(!prodMode);
        return this;
    }

    @Override
    public void setTest(boolean test) {
        super.setTest(test);
        this.prodMode = !test;
    }

    public boolean isDebug() {
        return debug;
    }

    public AdapayPayConfigStorage setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public boolean isVerifyRsa() {
        return verifyRsa;
    }

    public AdapayPayConfigStorage setVerifyRsa(boolean verifyRsa) {
        this.verifyRsa = verifyRsa;
        return this;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public AdapayPayConfigStorage setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public String getApiBase() {
        return apiBase;
    }

    public AdapayPayConfigStorage setApiBase(String apiBase) {
        this.apiBase = apiBase;
        return this;
    }

    public String getPageBase() {
        return pageBase;
    }

    public AdapayPayConfigStorage setPageBase(String pageBase) {
        this.pageBase = pageBase;
        return this;
    }

    public String getMerchantKey() {
        if (merchantKey != null && !merchantKey.isEmpty()) {
            return merchantKey;
        }
        if (generatedMerchantKey == null) {
            String source = String.valueOf(appId) + ":" + String.valueOf(apiKey) + ":" + String.valueOf(apiMockKey);
            generatedMerchantKey = "adapay_" + Integer.toHexString(source.hashCode());
        }
        return generatedMerchantKey;
    }

    public AdapayPayConfigStorage setMerchantKey(String merchantKey) {
        this.merchantKey = merchantKey;
        return this;
    }
}
