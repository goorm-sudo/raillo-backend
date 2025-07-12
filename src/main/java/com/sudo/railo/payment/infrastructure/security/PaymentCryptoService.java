package com.sudo.railo.payment.infrastructure.security;

import com.sudo.railo.payment.exception.PaymentCryptoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 결제 정보 암호화/복호화 서비스
 * 
 * 카드번호, 계좌번호 등 민감한 결제 정보를 안전하게 암호화하고 복호화하는 서비스
 * AES-256-GCM 알고리즘을 사용하여 기밀성과 무결성을 동시에 보장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCryptoService {

    private final PaymentCryptoConfig cryptoConfig;
    private final SecretKey secretKey;
    private final SecureRandom secureRandom;
    private final PaymentSecurityAuditService auditService;

    /**
     * 평문을 암호화
     * 
     * @param plainText 암호화할 평문
     * @return Base64로 인코딩된 암호문 (IV + 암호화된 데이터)
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }

        try {
            // IV 생성
            byte[] iv = cryptoConfig.generateIv(secureRandom);
            
            // 암호화 수행
            Cipher cipher = cryptoConfig.getEncryptCipher(secretKey, iv);
            byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            
            // IV와 암호화된 데이터를 함께 저장
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedData);
            
            String encrypted = Base64.getEncoder().encodeToString(byteBuffer.array());
            
            // 암호화 성공 로깅 (민감정보는 로깅하지 않음)
            log.debug("Successfully encrypted data. Length: {}", encrypted.length());
            
            return encrypted;
            
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new PaymentCryptoException("Failed to encrypt payment data", e);
        }
    }

    /**
     * 암호문을 복호화
     * 
     * @param encryptedText Base64로 인코딩된 암호문
     * @return 복호화된 평문
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return null;
        }

        try {
            // 복호화 권한 검증 및 감사 로깅
            auditService.logDecryptionAttempt();
            
            // Base64 디코딩
            byte[] encryptedData = Base64.getDecoder().decode(encryptedText);
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
            
            // IV 추출
            byte[] iv = new byte[12]; // GCM IV는 12바이트
            byteBuffer.get(iv);
            
            // 암호화된 데이터 추출
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);
            
            // 복호화 수행
            Cipher cipher = cryptoConfig.getDecryptCipher(secretKey, iv);
            byte[] decryptedData = cipher.doFinal(cipherText);
            
            String decrypted = new String(decryptedData, StandardCharsets.UTF_8);
            
            // 복호화 성공 감사 로깅
            auditService.logDecryptionSuccess();
            
            return decrypted;
            
        } catch (Exception e) {
            log.error("Decryption failed", e);
            auditService.logDecryptionFailure(e.getMessage());
            throw new PaymentCryptoException("Failed to decrypt payment data", e);
        }
    }

    /**
     * 카드번호 마스킹 (복호화 없이 수행)
     * 
     * @param cardNumber 카드번호
     * @return 마스킹된 카드번호 (예: **** **** **** 1234)
     */
    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        
        String cleaned = cardNumber.replaceAll("[^0-9]", "");
        if (cleaned.length() < 4) {
            return "****";
        }
        
        String lastFour = cleaned.substring(cleaned.length() - 4);
        return "**** **** **** " + lastFour;
    }

    /**
     * 계좌번호 마스킹 (복호화 없이 수행)
     * 
     * @param accountNumber 계좌번호
     * @return 마스킹된 계좌번호 (예: ****1234)
     */
    public String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        
        String cleaned = accountNumber.replaceAll("[^0-9]", "");
        if (cleaned.length() < 4) {
            return "****";
        }
        
        String lastFour = cleaned.substring(cleaned.length() - 4);
        return "****" + lastFour;
    }

    /**
     * 데이터 해싱 (검색용)
     * 결제수단 검색을 위한 단방향 해시 생성
     * 
     * @param data 해싱할 데이터
     * @return SHA-256 해시값
     */
    public String hash(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Hashing failed", e);
            throw new PaymentCryptoException("Failed to hash payment data", e);
        }
    }

    /**
     * 암호화 상태 검증
     * 주어진 텍스트가 암호화된 형식인지 확인
     */
    public boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        try {
            byte[] decoded = Base64.getDecoder().decode(text);
            // 최소한 IV(12바이트) + 데이터(1바이트) + 인증태그(16바이트) = 29바이트 이상이어야 함
            return decoded.length >= 29;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}