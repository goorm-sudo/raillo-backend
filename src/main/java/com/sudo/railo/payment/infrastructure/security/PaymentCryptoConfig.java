package com.sudo.railo.payment.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 결제 정보 암호화 설정 클래스
 * AES-256-GCM 알고리즘을 사용하여 민감한 결제 정보를 암호화
 */
@Configuration
public class PaymentCryptoConfig {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int AES_KEY_SIZE = 256;

    @Value("${payment.crypto.secret-key:}")
    private String secretKeyBase64;

    @Value("${payment.crypto.key-rotation-enabled:false}")
    private boolean keyRotationEnabled;

    /**
     * AES 비밀키 생성 또는 로드
     */
    @Bean
    public SecretKey paymentSecretKey() throws NoSuchAlgorithmException {
        if (secretKeyBase64 != null && !secretKeyBase64.isEmpty()) {
            try {
                // 환경변수나 설정에서 키를 로드
                byte[] decodedKey = Base64.getDecoder().decode(secretKeyBase64);
                
                // AES-256은 32바이트 키가 필요
                if (decodedKey.length != 32) {
                    throw new IllegalArgumentException(String.format(
                        "AES-256 requires exactly 32 bytes, but got %d bytes. " +
                        "Please use PaymentCryptoKeyGenerator to generate a valid key.",
                        decodedKey.length
                    ));
                }
                
                return new SecretKeySpec(decodedKey, ALGORITHM);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                    "Invalid payment crypto key format. " +
                    "Please ensure the key is properly Base64 encoded. " +
                    "Use PaymentCryptoKeyGenerator to generate a valid key. " +
                    "Error: " + e.getMessage(), e
                );
            }
        } else {
            // 키가 설정되지 않은 경우 명확한 가이드 제공
            String errorMessage = "\n" +
                "========================================\n" +
                "❌ Payment Crypto Key Configuration Missing!\n" +
                "========================================\n" +
                "Payment encryption requires a secret key to be configured.\n\n" +
                "To generate a new key, run:\n" +
                "  java -cp build/classes/java/main com.sudo.railo.payment.infrastructure.security.PaymentCryptoKeyGenerator\n\n" +
                "Then configure it in one of these ways:\n" +
                "1. Environment variable: export PAYMENT_CRYPTO_KEY=<generated-key>\n" +
                "2. application.yml: payment.crypto.secret-key: <generated-key>\n" +
                "3. System property: -Dpayment.crypto.secret-key=<generated-key>\n\n" +
                "⚠️ WARNING: Auto-generated keys are not persisted and will cause\n" +
                "   data loss on restart. Always configure a permanent key for production.\n" +
                "========================================\n";
            
            // 개발 환경에서만 자동 생성 허용 (경고 메시지와 함께)
            if ("local".equals(System.getProperty("spring.profiles.active")) || 
                "dev".equals(System.getProperty("spring.profiles.active"))) {
                System.err.println(errorMessage);
                System.err.println("⚠️ Generating temporary key for development. This is NOT suitable for production!");
                
                KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
                keyGenerator.init(AES_KEY_SIZE);
                SecretKey tempKey = keyGenerator.generateKey();
                
                // 생성된 키를 Base64로 인코딩하여 표시
                String tempKeyBase64 = Base64.getEncoder().encodeToString(tempKey.getEncoded());
                System.err.println("📌 Temporary key (save this for consistent encryption): " + tempKeyBase64);
                System.err.println("========================================\n");
                
                return tempKey;
            } else {
                // 프로덕션 환경에서는 키 누락 시 시작 실패
                throw new IllegalStateException(errorMessage);
            }
        }
    }

    /**
     * 보안 난수 생성기
     */
    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }

    /**
     * 암호화용 Cipher 인스턴스 생성
     */
    public Cipher getEncryptCipher(SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
        return cipher;
    }

    /**
     * 복호화용 Cipher 인스턴스 생성
     */
    public Cipher getDecryptCipher(SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
        return cipher;
    }

    /**
     * 초기화 벡터(IV) 생성
     */
    public byte[] generateIv(SecureRandom random) {
        byte[] iv = new byte[GCM_IV_LENGTH];
        random.nextBytes(iv);
        return iv;
    }

    /**
     * 키 로테이션 활성화 여부
     */
    public boolean isKeyRotationEnabled() {
        return keyRotationEnabled;
    }
}