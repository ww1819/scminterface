package com.scminterface.customer.zaoqiangTcm.msun.support;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;

/**
 * 众阳 OpenAPI SM2 签名（摘自 zhongyang demo，仅保留 HTTP 调用所需能力）。
 */
public final class MsunSm2Util
{
    private static final BouncyCastleProvider PROVIDER;
    private static final X9ECParameters PARAMETERS;
    private static final ECParameterSpec EC_PARAMETER_SPEC;
    private static final KeyFactory KEY_FACTORY;

    static
    {
        try
        {
            PROVIDER = new BouncyCastleProvider();
            PARAMETERS = GMNamedCurves.getByName("sm2p256v1");
            EC_PARAMETER_SPEC = new ECParameterSpec(
                    PARAMETERS.getCurve(), PARAMETERS.getG(), PARAMETERS.getN(), PARAMETERS.getH());
            KEY_FACTORY = KeyFactory.getInstance("EC", PROVIDER);
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("初始化 SM2 组件失败", ex);
        }
    }

    private MsunSm2Util()
    {
    }

    public static String sign(String plainText, String privateKeyHex)
            throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException
    {
        Signature signature = Signature.getInstance(GMObjectIdentifiers.sm2sign_with_sm3.toString(), PROVIDER);
        BigInteger bigInteger = new BigInteger(privateKeyHex, 16);
        BCECPrivateKey privateKey = (BCECPrivateKey) KEY_FACTORY.generatePrivate(
                new ECPrivateKeySpec(bigInteger, EC_PARAMETER_SPEC));
        signature.initSign(privateKey);
        signature.update(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }
}
