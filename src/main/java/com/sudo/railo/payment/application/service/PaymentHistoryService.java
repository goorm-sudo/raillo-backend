package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.application.dto.response.PaymentHistoryResponse;
import com.sudo.railo.payment.application.dto.response.PaymentInfoResponse;
import com.sudo.railo.payment.domain.entity.MileageTransaction;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.entity.PaymentExecutionStatus;
import com.sudo.railo.payment.domain.repository.MileageTransactionRepository;
import com.sudo.railo.payment.domain.repository.PaymentRepository;
import com.sudo.railo.payment.domain.service.NonMemberService;
import com.sudo.railo.payment.exception.PaymentValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 결제 내역 조회 애플리케이션 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentHistoryService {
    
    private final PaymentRepository paymentRepository;
    private final MileageTransactionRepository mileageTransactionRepository;
    private final NonMemberService nonMemberService;
    
    /**
     * 회원 결제 내역 조회
     */
    public PaymentHistoryResponse getPaymentHistory(
            Long memberId, 
            LocalDateTime startDate, 
            LocalDateTime endDate,
            String paymentMethod,
            Pageable pageable) {
        
        log.debug("회원 결제 내역 조회 - 회원ID: {}, 기간: {} ~ {}, 결제방법: {}", memberId, startDate, endDate, paymentMethod);
        
        // 1. DB에서 페이징된 결제 내역 조회
        Page<Payment> pagedPayments;
        if (startDate != null && endDate != null) {
            // 기간 지정된 경우 - DB 레벨에서 필터링 + 페이징
            pagedPayments = paymentRepository.findByMemberIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                memberId, startDate, endDate, pageable);
        } else {
            // 전체 기간 - DB 레벨에서 페이징만
            pagedPayments = paymentRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
        }
        
        // 2. 결제방법 필터링 (필요한 경우)
        List<Payment> payments = pagedPayments.getContent();
        if (paymentMethod != null && !paymentMethod.isEmpty()) {
            payments = payments.stream()
                    .filter(payment -> payment.getPaymentMethod().name().equals(paymentMethod))
                    .collect(Collectors.toList());
        }
        
        // 3. 마일리지 거래 내역 조회
        List<String> paymentIds = payments.stream()
                .map(payment -> payment.getPaymentId().toString())
                .collect(Collectors.toList());
        
        List<MileageTransaction> mileageTransactions = 
                mileageTransactionRepository.findByPaymentIds(paymentIds);
        
        // 4. 응답 DTO 생성
        List<PaymentHistoryResponse.PaymentHistoryItem> historyItems = 
                payments.stream()
                        .map(payment -> {
                            List<MileageTransaction> relatedMileageTransactions = 
                                    mileageTransactions.stream()
                                            .filter(mt -> payment.getPaymentId().toString().equals(mt.getPaymentId()))
                                            .collect(Collectors.toList());
                            
                            return PaymentHistoryResponse.PaymentHistoryItem.from(payment, relatedMileageTransactions);
                        })
                        .collect(Collectors.toList());
        
        // 5. 실제 페이징 정보로 응답 생성
        return PaymentHistoryResponse.builder()
                .payments(historyItems)
                .totalElements(pagedPayments.getTotalElements())
                .totalPages(pagedPayments.getTotalPages())
                .currentPage(pagedPayments.getNumber())
                .pageSize(pagedPayments.getSize())
                .hasNext(pagedPayments.hasNext())
                .hasPrevious(pagedPayments.hasPrevious())
                .build();
    }
    
    /**
     * 비회원 결제 내역 조회
     */
    public PaymentInfoResponse getNonMemberPayment(
            Long reservationId,
            String name,
            String phoneNumber,
            String password) {
        
        log.debug("비회원 결제 내역 조회 - 예약번호: {}, 이름: {}", reservationId, name);
        
        // 1. 예약번호로 결제 정보 조회
        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new PaymentValidationException("해당 예약의 결제 정보를 찾을 수 없습니다"));
        
        // 2. 회원 결제인지 확인
        if (payment.getMemberId() != null) {
            throw new PaymentValidationException("회원 결제입니다. 로그인 후 조회해주세요");
        }
        
        // 3. 비회원 정보 검증
        boolean isValid = nonMemberService.validateNonMemberInfo(name, phoneNumber, password, payment);
        
        if (!isValid) {
            log.warn("비회원 정보 검증 실패 - 예약번호: {}, 요청 이름: {}", reservationId, name);
            throw new PaymentValidationException("입력한 정보가 일치하지 않습니다");
        }
        
        // 4. 응답 생성
        return PaymentInfoResponse.builder()
                .paymentId(payment.getPaymentId())
                .reservationId(payment.getReservationId())
                .externalOrderId(payment.getExternalOrderId())
                .amountOriginalTotal(payment.getAmountOriginalTotal())
                .totalDiscountAmountApplied(payment.getTotalDiscountAmountApplied())
                .mileageAmountDeducted(payment.getMileageAmountDeducted())
                .amountPaid(payment.getAmountPaid())
                .paymentStatus(payment.getPaymentStatus())
                .paymentMethod(payment.getPaymentMethod().getDisplayName())
                .pgProvider(payment.getPgProvider())
                .pgTransactionId(payment.getPgTransactionId())
                .pgApprovalNo(payment.getPgApprovalNo())
                .receiptUrl(payment.getReceiptUrl())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .nonMemberName(payment.getNonMemberName())
                .nonMemberPhoneMasked(maskPhoneNumber(payment.getNonMemberPhone()))
                .build();
    }
    
    /**
     * 특정 결제의 상세 정보 조회 (회원용)
     */
    public PaymentInfoResponse getPaymentDetail(Long paymentId, Long memberId) {
        
        log.debug("결제 상세 정보 조회 - 결제ID: {}, 회원ID: {}", paymentId, memberId);
        
        // 1. 결제 정보 조회
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentValidationException("결제 정보를 찾을 수 없습니다"));
        
        // 2. 본인 결제인지 확인
        if (!payment.getMemberId().equals(memberId)) {
            throw new PaymentValidationException("본인의 결제 내역만 조회할 수 있습니다");
        }
        
        // 3. 마일리지 거래 내역 조회
        List<MileageTransaction> mileageTransactions = 
                mileageTransactionRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId.toString());
        
        // 4. 응답 생성
        return PaymentInfoResponse.builder()
                .paymentId(payment.getPaymentId())
                .reservationId(payment.getReservationId())
                .externalOrderId(payment.getExternalOrderId())
                .amountOriginalTotal(payment.getAmountOriginalTotal())
                .totalDiscountAmountApplied(payment.getTotalDiscountAmountApplied())
                .mileagePointsUsed(payment.getMileagePointsUsed())
                .mileageAmountDeducted(payment.getMileageAmountDeducted())
                .mileageToEarn(payment.getMileageToEarn())
                .amountPaid(payment.getAmountPaid())
                .paymentStatus(payment.getPaymentStatus())
                .paymentMethod(payment.getPaymentMethod().getDisplayName())
                .pgProvider(payment.getPgProvider())
                .pgTransactionId(payment.getPgTransactionId())
                .pgApprovalNo(payment.getPgApprovalNo())
                .receiptUrl(payment.getReceiptUrl())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .mileageTransactions(mileageTransactions.stream()
                        .map(this::convertToMileageInfo)
                        .collect(Collectors.toList()))
                .build();
    }
    
    /**
     * 전화번호 마스킹
     */
    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
    
    /**
     * MileageTransaction을 응답용 DTO로 변환
     */
    private PaymentInfoResponse.MileageTransactionInfo convertToMileageInfo(MileageTransaction transaction) {
        return PaymentInfoResponse.MileageTransactionInfo.builder()
                .transactionId(transaction.getTransactionId())
                .transactionType(transaction.getType().getDescription())
                .amount(transaction.getPointsAmount())
                .description(transaction.getDescription())
                .processedAt(transaction.getProcessedAt())
                .balanceAfter(transaction.getBalanceAfter())
                .build();
    }
} 