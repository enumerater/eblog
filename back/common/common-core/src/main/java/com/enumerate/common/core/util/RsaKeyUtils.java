package com.enumerate.common.core.util;

import lombok.experimental.UtilityClass;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 密钥工具类
 * 用于生成、加载 RSA 密钥对（供 JWT 签名/验签使用）
 *
 * 为何使用 RSA 而非 HMAC？
 *   - Gateway 只需要公钥即可验签，不持有私钥，降低私钥泄露风险
 *   - Auth Service 持有私钥用于签发，Gateway 持有公钥用于验证
 *   - 非对称密钥各自独立运维，Auth 轮转私钥不影响 Gateway
 */
@UtilityClass
public class RsaKeyUtils {

    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;

    /**
     * 生成 RSA 密钥对
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
            generator.initialize(KEY_SIZE, new SecureRandom());
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RSA 算法不可用", e);
        }
    }

    /**
     * 将 PublicKey 编码为 Base64 字符串
     */
    public static String publicKeyToBase64(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * 将 PrivateKey 编码为 Base64 字符串
     */
    public static String privateKeyToBase64(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    /**
     * 从 Base64 字符串加载 PublicKey
     */
    public static PublicKey publicKeyFromBase64(String base64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory factory = KeyFactory.getInstance(ALGORITHM);
            return factory.generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("RSA 公钥加载失败", e);
        }
    }

    /**
     * 从 Base64 字符串加载 PrivateKey
     */
    public static PrivateKey privateKeyFromBase64(String base64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory factory = KeyFactory.getInstance(ALGORITHM);
            return factory.generatePrivate(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("RSA 私钥加载失败", e);
        }
    }
}
