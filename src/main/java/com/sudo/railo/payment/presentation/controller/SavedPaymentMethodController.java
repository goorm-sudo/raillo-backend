package com.sudo.railo.payment.presentation.controller;

import com.sudo.railo.payment.application.dto.SavedPaymentMethodRequestDto;
import com.sudo.railo.payment.application.dto.SavedPaymentMethodResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/saved-payment-methods")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class SavedPaymentMethodController {

    private final SavedPaymentMethodJpaRepository savedPaymentMethodRepository;

    /**
     * 저장된 결제수단 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<SavedPaymentMethodResponseDto>> getSavedPaymentMethods(
            @RequestParam Long memberId) {
        
        log.info("저장된 결제수단 목록 조회 요청 - memberId: {}", memberId);
        
        List<SavedPaymentMethodEntity> savedMethods = savedPaymentMethodRepository.findByMemberIdAndIsActiveTrue(memberId);
        
        List<SavedPaymentMethodResponseDto> response = savedMethods.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
        
        log.info("조회된 결제수단 개수: {}", response.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 결제수단 저장
     */
    @PostMapping
    public ResponseEntity<SavedPaymentMethodResponseDto> savePaymentMethod(
            @RequestBody SavedPaymentMethodRequestDto request) {
        
        log.info("결제수단 저장 요청: {}", request);
        
        // 엔티티 생성
        SavedPaymentMethodEntity entity = createEntity(request);
        
        // DB에 저장
        SavedPaymentMethodEntity saved = savedPaymentMethodRepository.save(entity);
        
        // 응답 DTO 변환
        SavedPaymentMethodResponseDto response = convertToResponseDto(saved);
        
        log.info("결제수단 저장 완료: ID={}, 타입={}, 별명={}", 
                response.getId(), response.getPaymentMethodType(), response.getAlias());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 결제수단 삭제
     */
    @DeleteMapping("/{paymentMethodId}")
    public ResponseEntity<Void> deletePaymentMethod(@PathVariable Long paymentMethodId) {
        
        log.info("결제수단 삭제 요청 - paymentMethodId: {}", paymentMethodId);
        
        SavedPaymentMethodEntity entity = savedPaymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("결제수단을 찾을 수 없습니다: " + paymentMethodId));
        
        // 소프트 삭제 (is_active = false)
        entity.setIsActive(false);
        savedPaymentMethodRepository.save(entity);
        
        log.info("결제수단 삭제 완료: {}", paymentMethodId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * 기본 결제수단 설정
     */
    @PutMapping("/{paymentMethodId}/default")
    public ResponseEntity<Void> setDefaultPaymentMethod(
            @PathVariable Long paymentMethodId,
            @RequestParam Long memberId) {
        
        log.info("기본 결제수단 설정 요청 - paymentMethodId: {}, memberId: {}", paymentMethodId, memberId);
        
        // 기존 기본 결제수단 해제
        savedPaymentMethodRepository.updateAllToNonDefault(memberId);
        
        // 새로운 기본 결제수단 설정
        SavedPaymentMethodEntity entity = savedPaymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("결제수단을 찾을 수 없습니다: " + paymentMethodId));
        
        entity.setIsDefault(true);
        savedPaymentMethodRepository.save(entity);
        
        log.info("기본 결제수단 설정 완료: {}", paymentMethodId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * 결제용 원본 카드번호 조회 (내부 API)
     * 실제 결제 시에만 사용
     */
    @GetMapping("/{paymentMethodId}/raw")
    public ResponseEntity<SavedPaymentMethodResponseDto> getRawPaymentMethod(
            @PathVariable Long paymentMethodId,
            @RequestParam Long memberId) {
        
        log.info("결제용 원본 결제수단 조회 요청 - paymentMethodId: {}, memberId: {}", paymentMethodId, memberId);
        
        SavedPaymentMethodEntity entity = savedPaymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("결제수단을 찾을 수 없습니다: " + paymentMethodId));
        
        // 소유자 확인
        if (!entity.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        
        // 원본 데이터 반환 (마스킹 없음)
        SavedPaymentMethodResponseDto response = convertToRawResponseDto(entity);
        
        log.info("결제용 원본 결제수단 조회 완료 - ID: {}", paymentMethodId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 엔티티 생성
     */
    private SavedPaymentMethodEntity createEntity(SavedPaymentMethodRequestDto request) {
        SavedPaymentMethodEntity entity = new SavedPaymentMethodEntity();
        entity.setMemberId(request.getMemberId());
        entity.setPaymentMethodType(request.getPaymentMethodType());
        entity.setAlias(request.getAlias());
        entity.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        entity.setIsActive(true);
        entity.setCreatedAt(LocalDateTime.now());

        // 신용카드 정보 (원본 저장 - 조회 시 마스킹)
        if ("CREDIT_CARD".equals(request.getPaymentMethodType())) {
            entity.setCardNumber(request.getCardNumber()); // 원본 저장
            entity.setCardHolderName(request.getCardHolderName());
            entity.setCardExpiryMonth(request.getCardExpiryMonth());
            entity.setCardExpiryYear(request.getCardExpiryYear());
        }
        
        // 계좌 정보 (원본 저장 - 조회 시 마스킹)
        if ("BANK_ACCOUNT".equals(request.getPaymentMethodType())) {
            entity.setBankCode(request.getBankCode());
            entity.setAccountNumber(request.getAccountNumber()); // 원본 저장
            entity.setAccountHolderName(request.getAccountHolderName());
        }

        return entity;
    }
    
    /**
     * 엔티티를 응답 DTO로 변환
     */
    private SavedPaymentMethodResponseDto convertToResponseDto(SavedPaymentMethodEntity entity) {
        SavedPaymentMethodResponseDto dto = new SavedPaymentMethodResponseDto();
        dto.setId(entity.getId());
        dto.setMemberId(entity.getMemberId());
        dto.setPaymentMethodType(entity.getPaymentMethodType());
        dto.setAlias(entity.getAlias());
        dto.setIsDefault(entity.getIsDefault());
        dto.setCreatedAt(entity.getCreatedAt());
        
        // 신용카드 정보 (조회 시 마스킹 적용)
        if (entity.getCardNumber() != null) {
            dto.setCardNumber(maskCardNumber(entity.getCardNumber())); // 조회 시 마스킹
            dto.setCardHolderName(entity.getCardHolderName());
            dto.setCardExpiryMonth(entity.getCardExpiryMonth());
            dto.setCardExpiryYear(entity.getCardExpiryYear());
        }
        
        // 계좌 정보 (조회 시 마스킹 적용)
        if (entity.getAccountNumber() != null) {
            dto.setBankCode(entity.getBankCode());
            dto.setAccountNumber(maskAccountNumber(entity.getAccountNumber())); // 조회 시 마스킹
            dto.setAccountHolderName(entity.getAccountHolderName());
        }
        
        return dto;
    }
    
    /**
     * 엔티티를 원본 응답 DTO로 변환 (마스킹 없음)
     */
    private SavedPaymentMethodResponseDto convertToRawResponseDto(SavedPaymentMethodEntity entity) {
        SavedPaymentMethodResponseDto dto = new SavedPaymentMethodResponseDto();
        dto.setId(entity.getId());
        dto.setMemberId(entity.getMemberId());
        dto.setPaymentMethodType(entity.getPaymentMethodType());
        dto.setAlias(entity.getAlias());
        dto.setIsDefault(entity.getIsDefault());
        dto.setCreatedAt(entity.getCreatedAt());
        
        // 신용카드 정보 (원본 반환 - 마스킹 없음)
        if (entity.getCardNumber() != null) {
            dto.setCardNumber(entity.getCardNumber()); // 원본 반환
            dto.setCardHolderName(entity.getCardHolderName());
            dto.setCardExpiryMonth(entity.getCardExpiryMonth());
            dto.setCardExpiryYear(entity.getCardExpiryYear());
        }
        
        // 계좌 정보 (원본 반환 - 마스킹 없음)
        if (entity.getAccountNumber() != null) {
            dto.setBankCode(entity.getBankCode());
            dto.setAccountNumber(entity.getAccountNumber()); // 원본 반환
            dto.setAccountHolderName(entity.getAccountHolderName());
        }
        
        return dto;
    }
    
    /**
     * 카드번호 마스킹 처리
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        // 마지막 4자리만 보이도록 마스킹
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }
    
    /**
     * 계좌번호 마스킹 처리
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return accountNumber;
        }
        // 마지막 4자리만 보이도록 마스킹
        String lastFour = accountNumber.substring(accountNumber.length() - 4);
        return "****" + lastFour;
    }
}

/**
 * 저장된 결제수단 엔티티
 */
@Entity
@Table(name = "saved_payment_methods")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
class SavedPaymentMethodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "payment_method_type", nullable = false, length = 50)
    private String paymentMethodType;

    @Column(name = "alias", length = 100)
    private String alias;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // 신용카드 관련 필드
    @Column(name = "card_number", length = 20)
    private String cardNumber;

    @Column(name = "card_holder_name", length = 100)
    private String cardHolderName;

    @Column(name = "card_expiry_month", length = 2)
    private String cardExpiryMonth;

    @Column(name = "card_expiry_year", length = 4)
    private String cardExpiryYear;

    // 계좌 관련 필드
    @Column(name = "bank_code", length = 10)
    private String bankCode;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "account_holder_name", length = 100)
    private String accountHolderName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

/**
 * 저장된 결제수단 Repository
 */
@Repository
interface SavedPaymentMethodJpaRepository extends JpaRepository<SavedPaymentMethodEntity, Long> {
    
    List<SavedPaymentMethodEntity> findByMemberIdAndIsActiveTrue(Long memberId);
    
    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE SavedPaymentMethodEntity s SET s.isDefault = false WHERE s.memberId = :memberId AND s.isActive = true")
    void updateAllToNonDefault(@Param("memberId") Long memberId);
} 