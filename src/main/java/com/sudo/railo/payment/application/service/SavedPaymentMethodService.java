package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.application.dto.SavedPaymentMethodRequestDto;
import com.sudo.railo.payment.application.dto.SavedPaymentMethodResponseDto;
import com.sudo.railo.payment.domain.entity.SavedPaymentMethod;
import com.sudo.railo.payment.domain.repository.SavedPaymentMethodRepository;
import com.sudo.railo.payment.exception.PaymentNotFoundException;
import com.sudo.railo.payment.exception.PaymentValidationException;
import com.sudo.railo.payment.infrastructure.security.PaymentCryptoService;
import com.sudo.railo.payment.infrastructure.security.PaymentSecurityAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import com.sudo.railo.member.infra.MemberRepository;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.exception.MemberError;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 저장된 결제수단 관리 서비스
 * 
 * 민감한 결제 정보를 안전하게 암호화하여 저장하고 조회하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavedPaymentMethodService {

    private final SavedPaymentMethodRepository repository;
    private final PaymentCryptoService cryptoService;
    private final PaymentSecurityAuditService auditService;
    private final MemberRepository memberRepository;
    
    private static final String CURRENT_ENCRYPTION_VERSION = "1.0";

    /**
     * 새로운 결제수단 저장 (암호화 적용)
     */
    @Transactional
    public SavedPaymentMethodResponseDto savePaymentMethod(SavedPaymentMethodRequestDto request, UserDetails userDetails) {
        // UserDetails에서 회원 정보 추출
        Member member = memberRepository.findByMemberNo(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
        Long memberId = member.getId();
        
        // DTO에 memberId 설정
        request.setMemberId(memberId);
        
        log.info("Saving new payment method for member: {}", memberId);
        
        // 중복 체크 (해시값으로 검색)
        String hash = getHashForPaymentMethod(request);
        if (repository.existsByMemberIdAndHash(request.getMemberId(), hash)) {
            throw new IllegalArgumentException("이미 등록된 결제수단입니다.");
        }
        
        // 엔티티 생성 및 암호화 적용
        SavedPaymentMethod paymentMethod = buildEncryptedPaymentMethod(request);
        
        // 기본 결제수단 설정 처리
        if (request.getIsDefault() != null && request.getIsDefault()) {
            repository.updateAllToNotDefault(request.getMemberId());
            paymentMethod.setAsDefault();
        }
        
        SavedPaymentMethod saved = repository.save(paymentMethod);
        
        // 감사 로깅
        auditService.logPaymentMethodSaved(saved.getMemberId(), saved.getPaymentMethodType());
        
        return toResponseDto(saved, false);  // 저장 직후에는 마스킹된 데이터만 반환
    }
    
    /**
     * 회원의 저장된 결제수단 목록 조회 (마스킹 적용)
     */
    public List<SavedPaymentMethodResponseDto> getPaymentMethods(UserDetails userDetails) {
        // UserDetails에서 회원 정보 추출
        Member member = memberRepository.findByMemberNo(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
        Long memberId = member.getId();
        
        log.debug("Retrieving payment methods for member: {}", memberId);
        
        List<SavedPaymentMethod> methods = repository.findByMemberIdAndIsActive(memberId, true);
        
        return methods.stream()
                .map(method -> toResponseDto(method, false))  // 마스킹된 데이터만 반환
                .collect(Collectors.toList());
    }
    
    /**
     * 결제 실행을 위한 결제수단 조회 (복호화 적용)
     * 특별한 권한이 필요하며 감사 로그를 남김
     */
    @Transactional
    public SavedPaymentMethodResponseDto getPaymentMethodForPayment(Long paymentMethodId, UserDetails userDetails) {
        // UserDetails에서 회원 정보 추출
        Member member = memberRepository.findByMemberNo(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
        Long memberId = member.getId();
        
        log.info("Retrieving payment method for payment execution: {}", paymentMethodId);
        
        SavedPaymentMethod method = repository.findById(paymentMethodId)
                .orElseThrow(() -> new PaymentNotFoundException("결제수단을 찾을 수 없습니다."));
        
        // 소유자 확인
        if (!method.getMemberId().equals(memberId)) {
            auditService.logSecurityViolation("UNAUTHORIZED_ACCESS", 
                "Member " + memberId + " tried to access payment method " + paymentMethodId);
            throw new SecurityException("결제수단에 대한 접근 권한이 없습니다.");
        }
        
        // 사용 시간 업데이트
        method.updateLastUsedAt();
        repository.save(method);
        
        // 감사 로깅
        auditService.logSensitiveDataAccess("PAYMENT_METHOD", "PAYMENT_EXECUTION");
        
        return toResponseDto(method, true);  // 실제 결제 시에만 복호화된 데이터 반환
    }
    
    /**
     * 결제수단 삭제 (소프트 삭제)
     */
    @Transactional
    public void deletePaymentMethod(Long paymentMethodId, UserDetails userDetails) {
        // UserDetails에서 회원 정보 추출
        Member member = memberRepository.findByMemberNo(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
        Long memberId = member.getId();
        
        log.info("Deleting payment method: {}", paymentMethodId);
        
        SavedPaymentMethod method = repository.findById(paymentMethodId)
                .orElseThrow(() -> new PaymentNotFoundException("결제수단을 찾을 수 없습니다."));
        
        // 소유자 확인
        if (!method.getMemberId().equals(memberId)) {
            throw new SecurityException("결제수단에 대한 접근 권한이 없습니다.");
        }
        
        // 소프트 삭제
        method.deactivate();
        repository.save(method);
        
        // 감사 로깅
        auditService.logPaymentMethodDeleted(paymentMethodId);
    }
    
    /**
     * 기본 결제수단 설정
     */
    @Transactional
    public void setDefaultPaymentMethod(Long paymentMethodId, UserDetails userDetails) {
        // UserDetails에서 회원 정보 추출
        Member member = memberRepository.findByMemberNo(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
        Long memberId = member.getId();
        
        log.info("Setting default payment method: {}", paymentMethodId);
        
        SavedPaymentMethod method = repository.findById(paymentMethodId)
                .orElseThrow(() -> new PaymentNotFoundException("결제수단을 찾을 수 없습니다."));
        
        // 소유자 확인
        if (!method.getMemberId().equals(memberId)) {
            throw new SecurityException("결제수단에 대한 접근 권한이 없습니다.");
        }
        
        // 기존 기본 결제수단 해제
        repository.updateAllToNotDefault(memberId);
        
        // 새로운 기본 결제수단 설정
        method.setAsDefault();
        repository.save(method);
    }
    
    /**
     * 암호화된 결제수단 엔티티 생성
     */
    private SavedPaymentMethod buildEncryptedPaymentMethod(SavedPaymentMethodRequestDto request) {
        SavedPaymentMethod.SavedPaymentMethodBuilder builder = SavedPaymentMethod.builder()
                .memberId(request.getMemberId())
                .paymentMethodType(request.getPaymentMethodType())
                .alias(request.getAlias())
                .isDefault(false)
                .isActive(true)
                .encryptionVersion(CURRENT_ENCRYPTION_VERSION);
        
        // 카드 정보 암호화
        if (request.getCardNumber() != null) {
            String cleanCardNumber = request.getCardNumber().replaceAll("[^0-9]", "");
            builder.cardNumberEncrypted(cryptoService.encrypt(cleanCardNumber))
                   .cardNumberHash(cryptoService.hash(cleanCardNumber))
                   .cardLastFourDigits(cleanCardNumber.substring(cleanCardNumber.length() - 4));
            
            if (request.getCardHolderName() != null) {
                builder.cardHolderNameEncrypted(cryptoService.encrypt(request.getCardHolderName()));
            }
            if (request.getCardExpiryMonth() != null) {
                builder.cardExpiryMonthEncrypted(cryptoService.encrypt(request.getCardExpiryMonth()));
            }
            if (request.getCardExpiryYear() != null) {
                builder.cardExpiryYearEncrypted(cryptoService.encrypt(request.getCardExpiryYear()));
            }
        }
        
        // 계좌 정보 암호화
        if (request.getAccountNumber() != null) {
            String cleanAccountNumber = request.getAccountNumber().replaceAll("[^0-9]", "");
            builder.accountNumberEncrypted(cryptoService.encrypt(cleanAccountNumber))
                   .accountNumberHash(cryptoService.hash(cleanAccountNumber))
                   .accountLastFourDigits(cleanAccountNumber.substring(cleanAccountNumber.length() - 4))
                   .bankCode(request.getBankCode());
            
            if (request.getAccountHolderName() != null) {
                builder.accountHolderNameEncrypted(cryptoService.encrypt(request.getAccountHolderName()));
            }
            
            // 계좌 비밀번호 암호화
            if (request.getAccountPassword() != null) {
                builder.accountPasswordEncrypted(cryptoService.encrypt(request.getAccountPassword()));
            }
        }
        
        return builder.build();
    }
    
    /**
     * 엔티티를 응답 DTO로 변환
     */
    private SavedPaymentMethodResponseDto toResponseDto(SavedPaymentMethod entity, boolean includeDecrypted) {
        SavedPaymentMethodResponseDto.SavedPaymentMethodResponseDtoBuilder builder = 
            SavedPaymentMethodResponseDto.builder()
                .id(entity.getId())
                .memberId(entity.getMemberId())
                .paymentMethodType(entity.getPaymentMethodType())
                .alias(entity.getAlias())
                .isDefault(entity.getIsDefault())
                .isActive(entity.getIsActive())
                .lastUsedAt(entity.getLastUsedAt())
                .createdAt(entity.getCreatedAt());
        
        if (entity.isCard()) {
            builder.maskedCardNumber(entity.getMaskedCardNumber());
            
            if (includeDecrypted && entity.getCardNumberEncrypted() != null) {
                // 복호화 - 실제 결제 시에만
                builder.cardNumber(cryptoService.decrypt(entity.getCardNumberEncrypted()));
                if (entity.getCardHolderNameEncrypted() != null) {
                    builder.cardHolderName(cryptoService.decrypt(entity.getCardHolderNameEncrypted()));
                }
                if (entity.getCardExpiryMonthEncrypted() != null) {
                    builder.cardExpiryMonth(cryptoService.decrypt(entity.getCardExpiryMonthEncrypted()));
                }
                if (entity.getCardExpiryYearEncrypted() != null) {
                    builder.cardExpiryYear(cryptoService.decrypt(entity.getCardExpiryYearEncrypted()));
                }
            }
        }
        
        if (entity.isAccount()) {
            builder.maskedAccountNumber(entity.getMaskedAccountNumber())
                   .bankCode(entity.getBankCode());
            
            if (includeDecrypted && entity.getAccountNumberEncrypted() != null) {
                // 복호화 - 실제 결제 시에만
                builder.accountNumber(cryptoService.decrypt(entity.getAccountNumberEncrypted()));
                if (entity.getAccountHolderNameEncrypted() != null) {
                    builder.accountHolderName(cryptoService.decrypt(entity.getAccountHolderNameEncrypted()));
                }
            }
        }
        
        return builder.build();
    }
    
    /**
     * 결제수단 해시값 생성 (중복 체크용)
     */
    private String getHashForPaymentMethod(SavedPaymentMethodRequestDto request) {
        if (request.getCardNumber() != null) {
            return cryptoService.hash(request.getCardNumber().replaceAll("[^0-9]", ""));
        } else if (request.getAccountNumber() != null) {
            return cryptoService.hash(request.getAccountNumber().replaceAll("[^0-9]", ""));
        }
        return null;
    }
}