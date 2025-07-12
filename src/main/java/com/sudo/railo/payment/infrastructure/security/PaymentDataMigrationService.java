package com.sudo.railo.payment.infrastructure.security;

import com.sudo.railo.payment.domain.entity.SavedPaymentMethod;
import com.sudo.railo.payment.domain.repository.SavedPaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 결제 데이터 마이그레이션 서비스
 * 
 * 기존 평문 데이터를 암호화된 형식으로 변환하는 서비스
 * Zero-Downtime 마이그레이션을 위해 단계별로 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.crypto.migration.enabled", havingValue = "true")
public class PaymentDataMigrationService implements ApplicationRunner {

    private final SavedPaymentMethodRepository repository;
    private final PaymentCryptoService cryptoService;
    private final PaymentSecurityAuditService auditService;
    
    private static final int BATCH_SIZE = 100;
    private static final String MIGRATION_VERSION = "1.0";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting payment data migration...");
        migrateAllPaymentMethods();
    }

    /**
     * 모든 결제수단 데이터 마이그레이션
     */
    @Transactional
    public void migrateAllPaymentMethods() {
        int pageNumber = 0;
        int migratedCount = 0;
        int errorCount = 0;
        
        try {
            Page<SavedPaymentMethod> page;
            do {
                page = findPaymentMethodsToMigrate(PageRequest.of(pageNumber, BATCH_SIZE));
                List<SavedPaymentMethod> migratedMethods = new ArrayList<>();
                
                for (SavedPaymentMethod method : page.getContent()) {
                    try {
                        if (needsMigration(method)) {
                            migratePaymentMethod(method);
                            migratedMethods.add(method);
                            migratedCount++;
                        }
                    } catch (Exception e) {
                        log.error("Failed to migrate payment method ID: {}", method.getId(), e);
                        errorCount++;
                    }
                }
                
                // 배치 단위로 저장
                if (!migratedMethods.isEmpty()) {
                    for (SavedPaymentMethod method : migratedMethods) {
                        repository.save(method);
                    }
                    log.info("Migrated batch of {} payment methods", migratedMethods.size());
                }
                
                pageNumber++;
            } while (page.hasNext());
            
            log.info("Payment data migration completed. Migrated: {}, Errors: {}", migratedCount, errorCount);
            auditService.logDataMigration("PAYMENT_METHOD_ENCRYPTION", migratedCount);
            
        } catch (Exception e) {
            log.error("Payment data migration failed", e);
            throw new RuntimeException("Failed to migrate payment data", e);
        }
    }

    /**
     * 단일 결제수단 마이그레이션
     */
    private void migratePaymentMethod(SavedPaymentMethod method) {
        // 카드 정보 마이그레이션
        if (method.isCard() && hasPlainCardData(method)) {
            migrateCardData(method);
        }
        
        // 계좌 정보 마이그레이션
        if (method.isAccount() && hasPlainAccountData(method)) {
            migrateAccountData(method);
        }
        
        // 암호화 버전 설정
        method.updateEncryptionVersion(MIGRATION_VERSION);
    }

    /**
     * 카드 데이터 마이그레이션
     */
    @SuppressWarnings("deprecation")
    private void migrateCardData(SavedPaymentMethod method) {
        // 레거시 필드에서 데이터 추출 (임시 접근)
        String plainCardNumber = getPlainCardNumber(method);
        if (plainCardNumber != null && !plainCardNumber.isEmpty()) {
            String cleanNumber = plainCardNumber.replaceAll("[^0-9]", "");
            
            // 암호화 및 해시 생성
            String encryptedNumber = cryptoService.encrypt(cleanNumber);
            String numberHash = cryptoService.hash(cleanNumber);
            String lastFourDigits = cleanNumber.substring(cleanNumber.length() - 4);
            
            // 카드 소유자명 암호화
            String holderName = getPlainCardHolderName(method);
            String encryptedHolderName = holderName != null ? cryptoService.encrypt(holderName) : null;
            
            // 유효기간 암호화
            String expiryMonth = getPlainCardExpiryMonth(method);
            String encryptedExpiryMonth = expiryMonth != null ? cryptoService.encrypt(expiryMonth) : null;
            
            String expiryYear = getPlainCardExpiryYear(method);
            String encryptedExpiryYear = expiryYear != null ? cryptoService.encrypt(expiryYear) : null;
            
            // 비즈니스 메서드를 통해 설정
            method.setEncryptedCardInfo(encryptedNumber, numberHash, lastFourDigits,
                                        encryptedHolderName, encryptedExpiryMonth, encryptedExpiryYear);
            
            // 레거시 필드 제거 준비 (null 설정)
            clearLegacyCardFields(method);
        }
    }

    /**
     * 계좌 데이터 마이그레이션
     */
    @SuppressWarnings("deprecation")
    private void migrateAccountData(SavedPaymentMethod method) {
        // 레거시 필드에서 데이터 추출 (임시 접근)
        String plainAccountNumber = getPlainAccountNumber(method);
        if (plainAccountNumber != null && !plainAccountNumber.isEmpty()) {
            String cleanNumber = plainAccountNumber.replaceAll("[^0-9]", "");
            
            // 암호화 및 해시 생성
            String encryptedNumber = cryptoService.encrypt(cleanNumber);
            String numberHash = cryptoService.hash(cleanNumber);
            String lastFourDigits = cleanNumber.substring(cleanNumber.length() - 4);
            
            // 계좌 소유자명 암호화
            String holderName = getPlainAccountHolderName(method);
            String encryptedHolderName = holderName != null ? cryptoService.encrypt(holderName) : null;
            
            // 비즈니스 메서드를 통해 설정
            method.setEncryptedAccountInfo(encryptedNumber, numberHash, lastFourDigits,
                                           encryptedHolderName, null);
            
            // 레거시 필드 제거 준비 (null 설정)
            clearLegacyAccountFields(method);
        }
    }

    /**
     * 마이그레이션이 필요한지 확인
     */
    private boolean needsMigration(SavedPaymentMethod method) {
        // 암호화 버전이 없거나 현재 버전보다 낮으면 마이그레이션 필요
        return method.getEncryptionVersion() == null || 
               !MIGRATION_VERSION.equals(method.getEncryptionVersion());
    }

    /**
     * 평문 카드 데이터가 있는지 확인
     */
    private boolean hasPlainCardData(SavedPaymentMethod method) {
        // 암호화된 필드가 비어있고 레거시 필드에 데이터가 있는 경우
        return method.getCardNumberEncrypted() == null && getPlainCardNumber(method) != null;
    }

    /**
     * 평문 계좌 데이터가 있는지 확인
     */
    private boolean hasPlainAccountData(SavedPaymentMethod method) {
        // 암호화된 필드가 비어있고 레거시 필드에 데이터가 있는 경우
        return method.getAccountNumberEncrypted() == null && getPlainAccountNumber(method) != null;
    }

    // 레거시 필드 접근 메서드들 (리플렉션 사용 회피를 위한 임시 메서드)
    // 실제 구현 시에는 엔티티에 @Deprecated 레거시 getter 추가 필요
    
    private String getPlainCardNumber(SavedPaymentMethod method) {
        // TODO: 레거시 필드 접근 로직
        return null;
    }
    
    private String getPlainCardHolderName(SavedPaymentMethod method) {
        // TODO: 레거시 필드 접근 로직
        return null;
    }
    
    private String getPlainCardExpiryMonth(SavedPaymentMethod method) {
        // TODO: 레거시 필드 접근 로직
        return null;
    }
    
    private String getPlainCardExpiryYear(SavedPaymentMethod method) {
        // TODO: 레거시 필드 접근 로직
        return null;
    }
    
    private String getPlainAccountNumber(SavedPaymentMethod method) {
        // TODO: 레거시 필드 접근 로직
        return null;
    }
    
    private String getPlainAccountHolderName(SavedPaymentMethod method) {
        // TODO: 레거시 필드 접근 로직
        return null;
    }
    
    private void clearLegacyCardFields(SavedPaymentMethod method) {
        // TODO: 레거시 필드 null 설정
    }
    
    private void clearLegacyAccountFields(SavedPaymentMethod method) {
        // TODO: 레거시 필드 null 설정
    }
    
    @SuppressWarnings("unchecked")
    private Page<SavedPaymentMethod> findPaymentMethodsToMigrate(PageRequest pageRequest) {
        // JPA Repository의 findAll 메서드 사용
        return (Page<SavedPaymentMethod>) repository.findAll(pageRequest);
    }
    
}