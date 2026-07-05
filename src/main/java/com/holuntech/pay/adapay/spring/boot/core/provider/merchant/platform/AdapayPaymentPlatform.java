package com.holuntech.pay.adapay.spring.boot.core.provider.merchant.platform;

import com.egzosn.pay.common.api.PayConfigStorage;
import com.egzosn.pay.common.api.PayService;
import com.egzosn.pay.common.bean.TransactionType;
import com.egzosn.pay.common.http.HttpConfigStorage;
import com.egzosn.pay.common.util.str.StringUtils;
import com.egzosn.pay.spring.boot.core.merchant.PaymentPlatform;
import com.egzosn.pay.spring.boot.core.merchant.bean.CommonPaymentPlatformMerchantDetails;
import com.holuntech.pay.adapay.api.AdapayPayConfigStorage;
import com.holuntech.pay.adapay.api.AdapayPayService;
import com.holuntech.pay.adapay.bean.AdapayTransactionType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * Adapay支付平台。
 *
 * @author egan
 */
@AutoConfiguration(AdapayPaymentPlatform.PLATFORM_NAME)
@ConditionalOnClass(name = "com.holuntech.pay.adapay.api.AdapayPayConfigStorage")
public class AdapayPaymentPlatform implements PaymentPlatform {

    public static final String PLATFORM_NAME = "adapay";
    @Deprecated
    public static final String platformName = PLATFORM_NAME;

    @Override
    public String getPlatform() {
        return PLATFORM_NAME;
    }

    @Override
    public PayService getPayService(PayConfigStorage payConfigStorage) {
        if (payConfigStorage instanceof AdapayPayConfigStorage) {
            return new AdapayPayService((AdapayPayConfigStorage) payConfigStorage);
        }

        AdapayPayConfigStorage configStorage = new AdapayPayConfigStorage();
        configStorage.setInputCharset(payConfigStorage.getInputCharset());
        configStorage.setAppId(payConfigStorage.getAppId());
        configStorage.setApiKey(firstNotEmpty(payConfigStorage.getPid(), attr(payConfigStorage, "api_key")));
        configStorage.setApiMockKey(firstNotEmpty(attr(payConfigStorage, "api_mock_key"), attr(payConfigStorage, "api_key_test")));
        configStorage.setRsaPrivateKey(firstNotEmpty(payConfigStorage.getKeyPrivate(),
                firstNotEmpty(attr(payConfigStorage, "key_private"), attr(payConfigStorage, "private_key"))));
        configStorage.setRsaPublicKey(firstNotEmpty(payConfigStorage.getKeyPublic(),
                firstNotEmpty(attr(payConfigStorage, "key_public"), attr(payConfigStorage, "rsa_public_key"))));
        configStorage.setNotifyUrl(payConfigStorage.getNotifyUrl());
        configStorage.setReturnUrl(payConfigStorage.getReturnUrl());
        configStorage.setPayType(payConfigStorage.getPayType());
        configStorage.setSignType(payConfigStorage.getSignType());
        configStorage.setProdMode(!parseBoolean(attr(payConfigStorage, "is_test"), payConfigStorage.isTest()));

        String merchantKey = firstNotEmpty(attr(payConfigStorage, "merchant_key"), attr(payConfigStorage, "merchantKey"));
        if (StringUtils.isNotEmpty(merchantKey)) {
            configStorage.setMerchantKey(merchantKey);
        }

        if (payConfigStorage instanceof CommonPaymentPlatformMerchantDetails) {
            CommonPaymentPlatformMerchantDetails details = (CommonPaymentPlatformMerchantDetails) payConfigStorage;
            if (StringUtils.isNotEmpty(details.getSeller())) {
                configStorage.setMerchantKey(details.getSeller());
            }
        }

        return new AdapayPayService(configStorage);
    }

    @Override
    public PayService getPayService(PayConfigStorage payConfigStorage, HttpConfigStorage httpConfigStorage) {
        PayService payService = getPayService(payConfigStorage);
        payService.setRequestTemplateConfigStorage(httpConfigStorage);
        return payService;
    }

    @Override
    public TransactionType getTransactionType(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        return AdapayTransactionType.of(name);
    }

    private static String attr(PayConfigStorage payConfigStorage, String name) {
        Object value = payConfigStorage.getAttr(name);
        return value == null ? null : value.toString();
    }

    private static String firstNotEmpty(String first, String second) {
        return StringUtils.isNotEmpty(first) ? first : second;
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        return StringUtils.isEmpty(value) ? defaultValue : Boolean.parseBoolean(value);
    }
}
