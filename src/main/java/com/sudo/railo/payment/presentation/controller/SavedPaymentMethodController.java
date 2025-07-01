package com.sudo.railo.payment.presentation.controller;

import com.sudo.railo.payment.application.dto.SavedPaymentMethodRequestDto;
import com.sudo.railo.payment.application.dto.SavedPaymentMethodResponseDto;
import com.sudo.railo.payment.domain.entity.SavedPaymentMethod;
import com.sudo.railo.payment.infrastructure.persistence.SavedPaymentMethodRepository;
import com.sudo.railo.member.application.util.MemberUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/saved-payment-methods")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class SavedPaymentMethodController {

    private final SavedPaymentMethodRepository savedPaymentMethodRepository;
    private final MemberUtil memberUtil;

    /**
     * 저장된 결제수단 목록 조회
     * JWT 토큰에서 memberId를 자동으로 추출하여 사용
     */
    @GetMapping
    public ResponseEntity<List<SavedPaymentMethodResponseDto>> getSavedPaymentMethods() {
        
        // JWT 토큰에서 현재 로그인한 사용자의 memberId 추출
        Long memberId = memberUtil.getCurrentMemberId();
        
        log.debug("저장된 결제수단 목록 조회 요청 - memberId: {}", memberId);
        
        List<SavedPaymentMethod> savedMethods = savedPaymentMethodRepository.findByMemberIdAndIsActiveTrue(memberId);
        
        List<SavedPaymentMethodResponseDto> response = savedMethods.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
        
        log.info("조회된 결제수단 개수: {}", response.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 결제수단 저장
     * JWT 토큰에서 memberId를 자동으로 추출하여 사용
     */
    @PostMapping
    public ResponseEntity<SavedPaymentMethodResponseDto> savePaymentMethod(
            @RequestBody SavedPaymentMethodRequestDto request) {
        
        // JWT 토큰에서 현재 로그인한 사용자의 memberId 추출
        Long memberId = memberUtil.getCurrentMemberId();
        
        log.debug("결제수단 저장 요청 - memberId: {}, 타입: {}", memberId, request.getPaymentMethodType());
        
        // 요청 DTO에 memberId 설정
        request.setMemberId(memberId);
        
        // 엔티티 생성
        SavedPaymentMethod entity = createEntity(request);
        
        // DB에 저장
        SavedPaymentMethod saved = savedPaymentMethodRepository.save(entity);
        
        // 응답 DTO 변환
        SavedPaymentMethodResponseDto response = convertToResponseDto(saved);
        
        log.debug("결제수단 저장 완료: ID={}, 타입={}, 별명={}", 
                response.getId(), response.getPaymentMethodType(), response.getAlias());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 결제수단 삭제
     * JWT 토큰에서 memberId를 자동으로 추출하여 소유권 검증
     */
    @DeleteMapping("/{paymentMethodId}")
    public ResponseEntity<Void> deletePaymentMethod(@PathVariable Long paymentMethodId) {
        
        // JWT 토큰에서 현재 로그인한 사용자의 memberId 추출
        Long memberId = memberUtil.getCurrentMemberId();
        
        log.debug("결제수단 삭제 요청 - paymentMethodId: {}, memberId: {}", paymentMethodId, memberId);
        
        SavedPaymentMethod entity = savedPaymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("결제수단을 찾을 수 없습니다: " + paymentMethodId));
        
        // 소유권 검증
        if (!entity.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 결제수단만 삭제할 수 있습니다.");
        }
        
        // 소프트 삭제 (is_active = false)
        entity.setIsActive(false);
        savedPaymentMethodRepository.save(entity);
        
        log.debug("결제수단 삭제 완료: {}", paymentMethodId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * 기본 결제수단 설정
     * JWT 토큰에서 memberId를 자동으로 추출하여 소유권 검증
     */
    @PutMapping("/{paymentMethodId}/default")
    public ResponseEntity<Void> setDefaultPaymentMethod(@PathVariable Long paymentMethodId) {
        
        // JWT 토큰에서 현재 로그인한 사용자의 memberId 추출
        Long memberId = memberUtil.getCurrentMemberId();
        
        log.debug("기본 결제수단 설정 요청 - paymentMethodId: {}, memberId: {}", paymentMethodId, memberId);
        
        // 기존 기본 결제수단 해제
        savedPaymentMethodRepository.updateAllToNonDefault(memberId);
        
        // 새로운 기본 결제수단 설정
        SavedPaymentMethod entity = savedPaymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("결제수단을 찾을 수 없습니다: " + paymentMethodId));
        
        // 소유권 검증
        if (!entity.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 결제수단만 설정할 수 있습니다.");
        }
        
        entity.setIsDefault(true);
        savedPaymentMethodRepository.save(entity);
        
        log.debug("기본 결제수단 설정 완료: {}", paymentMethodId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * 결제용 원본 카드번호 조회 (내부 API)
     * 실제 결제 시에만 사용
     */
    @GetMapping("/{paymentMethodId}/raw")
    public ResponseEntity<SavedPaymentMethodResponseDto> getRawPaymentMethod(@PathVariable Long paymentMethodId) {
        
        // JWT 토큰에서 현재 로그인한 사용자의 memberId 추출
        Long memberId = memberUtil.getCurrentMemberId();
        
        log.debug("결제용 원본 결제수단 조회 요청 - paymentMethodId: {}, memberId: {}", paymentMethodId, memberId);
        
        SavedPaymentMethod entity = savedPaymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("결제수단을 찾을 수 없습니다: " + paymentMethodId));
        
        // 소유권 검증
        if (!entity.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 결제수단만 조회할 수 있습니다.");
        }
        
        // 원본 데이터 반환 (마스킹 없음)
        SavedPaymentMethodResponseDto response = convertToRawResponseDto(entity);
        
        log.debug("결제용 원본 결제수단 조회 완료 - ID: {}", paymentMethodId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 엔티티 생성
     */
    private SavedPaymentMethod createEntity(SavedPaymentMethodRequestDto request) {
        return SavedPaymentMethod.builder()
                .memberId(request.getMemberId())
                .paymentMethodType(request.getPaymentMethodType())
                .alias(request.getAlias())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .isActive(true)
                .cardNumber(request.getCardNumber())
                .cardHolderName(request.getCardHolderName())
                .cardExpiryMonth(request.getCardExpiryMonth())
                .cardExpiryYear(request.getCardExpiryYear())
                .bankCode(request.getBankCode())
                .accountNumber(request.getAccountNumber())
                .accountHolderName(request.getAccountHolderName())
                .build();
    }
    
    /**
     * 엔티티를 응답 DTO로 변환
     */
    private SavedPaymentMethodResponseDto convertToResponseDto(SavedPaymentMethod entity) {
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
    private SavedPaymentMethodResponseDto convertToRawResponseDto(SavedPaymentMethod entity) {
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