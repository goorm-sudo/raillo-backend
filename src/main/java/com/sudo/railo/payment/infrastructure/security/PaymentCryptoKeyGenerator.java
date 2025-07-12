package com.sudo.railo.payment.infrastructure.security;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 결제 암호화 키 생성 유틸리티
 * AES-256 암호화를 위한 32바이트 키를 안전하게 생성하고 관리하는 도구
 */
public class PaymentCryptoKeyGenerator {
    
    private static final String ALGORITHM = "AES";
    private static final int AES_KEY_SIZE = 256;
    private static final int KEY_LENGTH_BYTES = 32; // AES-256은 32바이트 필요
    
    /**
     * 새로운 AES-256 암호화 키 생성
     * 
     * @return Base64로 인코딩된 32바이트 키
     * @throws NoSuchAlgorithmException AES 알고리즘을 사용할 수 없는 경우
     */
    public static String generateNewKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(AES_KEY_SIZE, new SecureRandom());
        SecretKey secretKey = keyGenerator.generateKey();
        
        byte[] keyBytes = secretKey.getEncoded();
        return Base64.getEncoder().encodeToString(keyBytes);
    }
    
    /**
     * Base64 인코딩된 키의 유효성 검증
     * 
     * @param base64Key Base64로 인코딩된 키
     * @return 유효한 AES-256 키인지 여부
     */
    public static boolean isValidKey(String base64Key) {
        if (base64Key == null || base64Key.isEmpty()) {
            return false;
        }
        
        try {
            byte[] decodedKey = Base64.getDecoder().decode(base64Key);
            return decodedKey.length == KEY_LENGTH_BYTES;
        } catch (IllegalArgumentException e) {
            // Base64 디코딩 실패
            return false;
        }
    }
    
    /**
     * 키 생성 및 설정 가이드 출력
     * 개발자가 초기 설정 시 사용할 수 있는 가이드 제공
     */
    public static void printSetupGuide() {
        System.out.println("=== 결제 암호화 키 설정 가이드 ===");
        System.out.println();
        
        try {
            String newKey = generateNewKey();
            System.out.println("1. 새로운 암호화 키가 생성되었습니다:");
            System.out.println("   " + newKey);
            System.out.println();
            
            System.out.println("2. 환경별 설정 방법:");
            System.out.println();
            
            System.out.println("   [개발 환경 - application-local.yml]");
            System.out.println("   payment:");
            System.out.println("     crypto:");
            System.out.println("       secret-key: " + newKey);
            System.out.println();
            
            System.out.println("   [운영 환경 - 환경변수]");
            System.out.println("   export PAYMENT_CRYPTO_KEY=" + newKey);
            System.out.println();
            
            System.out.println("   [Docker Compose]");
            System.out.println("   environment:");
            System.out.println("     - PAYMENT_CRYPTO_KEY=" + newKey);
            System.out.println();
            
            System.out.println("3. 주의사항:");
            System.out.println("   - 이 키는 민감한 결제 정보를 암호화하는 데 사용됩니다");
            System.out.println("   - 운영 환경에서는 반드시 안전한 방법으로 관리하세요");
            System.out.println("   - 키를 변경하면 기존 암호화된 데이터를 복호화할 수 없습니다");
            System.out.println("   - 키 로테이션이 필요한 경우 별도의 마이그레이션 전략이 필요합니다");
            System.out.println();
            
            System.out.println("4. 보안 권장사항:");
            System.out.println("   - Git에 커밋하지 마세요 (.gitignore에 추가)");
            System.out.println("   - AWS Secrets Manager, HashiCorp Vault 등 사용 권장");
            System.out.println("   - 정기적인 키 로테이션 정책 수립");
            System.out.println("   - 접근 권한을 최소한으로 제한");
            
        } catch (NoSuchAlgorithmException e) {
            System.err.println("❌ 키 생성 실패: " + e.getMessage());
        }
        
        System.out.println("===========================");
    }
    
    /**
     * 메인 메소드 - 직접 실행하여 새 키 생성
     */
    public static void main(String[] args) {
        System.out.println("Payment Crypto Key Generator v1.0");
        System.out.println("---------------------------------");
        
        if (args.length > 0 && "--validate".equals(args[0])) {
            if (args.length < 2) {
                System.err.println("사용법: java PaymentCryptoKeyGenerator --validate <base64-key>");
                System.exit(1);
            }
            
            String keyToValidate = args[1];
            boolean isValid = isValidKey(keyToValidate);
            
            System.out.println("키 검증 결과: " + (isValid ? "✅ 유효함" : "❌ 유효하지 않음"));
            if (!isValid) {
                System.out.println("AES-256 키는 정확히 32바이트여야 합니다.");
            }
        } else {
            // 기본 동작: 설정 가이드 출력
            printSetupGuide();
        }
    }
}