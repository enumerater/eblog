package com.enumerate.auth.config;

import com.enumerate.common.core.util.RsaKeyUtils;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * RSA 密钥对管理器
 *
 * 策略:
 *  1. 如果配置文件中已有密钥对 (Base64 格式), 直接加载
 *  2. 如果未配置, 自动生成并在日志输出 Base64 编码
 *     运维人员可将日志中的密钥对拷贝到 Gateway 和 Auth 的配置中
 *  3. 生产环境建议使用 Nacos 配置中心管理密钥 (动态刷新)
 */
@Slf4j
@Component
public class KeyPairManager {

    private final JwtProperties jwtProperties;

    @Getter
    private PrivateKey privateKey;

    @Getter
    private PublicKey publicKey;

    public KeyPairManager(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        String privPem = jwtProperties.getPrivateKey();
        String pubPem = jwtProperties.getPublicKey();

        if (privPem != null && !privPem.isBlank() && pubPem != null && !pubPem.isBlank()) {
            // 从配置加载
            privateKey = RsaKeyUtils.privateKeyFromBase64(privPem);
            publicKey = RsaKeyUtils.publicKeyFromBase64(pubPem);
            log.info("RSA 密钥对已从配置加载");
        } else {
            // 自动生成
            var keyPair = RsaKeyUtils.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();

            String privB64 = RsaKeyUtils.privateKeyToBase64(privateKey);
            String pubB64 = RsaKeyUtils.publicKeyToBase64(publicKey);
            log.info("═══════════════════════════════════════════════════");
            log.info("  新 RSA 密钥对已生成, 请保存到配置:");
            log.info("  JWT_PUBLIC_KEY={}", pubB64);
            log.info("  JWT_PRIVATE_KEY={}", privB64);
            log.info("  (注意: 请将公钥同步配置到 Gateway 的 jwt.public-key)");
            log.info("═══════════════════════════════════════════════════");
        }
    }
}
