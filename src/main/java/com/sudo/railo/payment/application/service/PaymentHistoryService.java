package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.application.dto.response.PaymentHistoryResponse;
import com.sudo.railo.payment.application.dto.response.PaymentInfoResponse;
import com.sudo.railo.payment.domain.entity.MileageTransaction;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.entity.PaymentExecutionStatus;
import com.sudo.railo.payment.domain.entity.RefundCalculation;
import com.sudo.railo.payment.application.port.out.LoadMileageTransactionPort;
import com.sudo.railo.payment.application.port.out.LoadPaymentPort;
import com.sudo.railo.payment.application.port.out.LoadRefundCalculationPort;
import com.sudo.railo.payment.domain.service.NonMemberService;
import com.sudo.railo.payment.exception.PaymentValidationException;
import com.sudo.railo.payment.exception.PaymentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.sudo.railo.member.infra.MemberRepository;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.exception.MemberError;

/**
 * 결제 내역 조회 애플리케이션 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentHistoryService {
    
    private final LoadPaymentPort loadPaymentPort;
    private final LoadMileageTransactionPort loadMileageTransactionPort;
    private final LoadRefundCalculationPort loadRefundCalculationPort;
    private final NonMemberService nonMemberService;
    private final MemberRepository memberRepository;
    
    /**
     * 회원 결제 내역 조회
     */
    public PaymentHistoryResponse getPaymentHistory(
            UserDetails userDetails, 
            LocalDateTime startDate, 
            LocalDateTime endDate,
            String paymentMethod,
            Pageable pageable) {
        
        // UserDetails에서 회원 정보 추출
        Member member = memberRepository.findByMemberNo(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
        Long memberId = member.getId();
        
        log.debug("회원 결제 내역 조회 - 회원ID: {}, 기간: {} ~ {}, 결제방법: {}", memberId, startDate, endDate, paymentMethod);
        
        // 1. DB에서 페이징된 결제 내역 조회
        Page<Payment> pagedPayments;
        if (startDate != null && endDate != null) {
            // 기간 지정된 경우 - DB 레벨에서 필터링 + 페이징
            pagedPayments = loadPaymentPort.findByMemberIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                memberId, startDate, endDate, pageable);
        } else {
            // 전체 기간 - DB 레벨에서 페이징만
            pagedPayments = loadPaymentPort.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
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
                .map(payment -> payment.getId().toString())
                .collect(Collectors.toList());
        
        List<MileageTransaction> mileageTransactions = 
                loadMileageTransactionPort.findByPaymentIds(paymentIds);
        
        // 3-1. 환불 정보 조회
        List<Long> paymentIdsLong = payments.stream()
                .map(Payment::getId)
                .collect(Collectors.toList());
        
        List<RefundCalculation> refundCalculations = 
                loadRefundCalculationPort.findByPaymentIds(paymentIdsLong);
        
        // 4. 응답 DTO 생성
        List<PaymentHistoryResponse.PaymentHistoryItem> historyItems = 
                payments.stream()
                        .map(payment -> {
                            List<MileageTransaction> relatedMileageTransactions = 
                                    mileageTransactions.stream()
                                            .filter(mt -> payment.getId().toString().equals(mt.getId()))
                                            .collect(Collectors.toList());
                            
                            RefundCalculation refundCalculation = refundCalculations.stream()
                                    .filter(rc -> rc.getPaymentId().equals(payment.getId()))
                                    .findFirst()
                                    .orElse(null);
                            
                            return PaymentHistoryResponse.PaymentHistoryItem.from(payment, relatedMileageTransactions, refundCalculation);
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
        Payment payment = loadPaymentPort.findByReservationId(reservationId)
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
                .paymentId(payment.getId())
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
     * 특정 예약번호로 결제 정보 조회 (회원용)
     * 본인 소유 여부 검증 포함
     */
    public PaymentInfoResponse getPaymentByReservationId(Long reservationId, UserDetails userDetails) {
        
        // UserDetails에서 회원 정보 추출
        Member member = memberRepository.findByMemberNo(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
        Long memberId = member.getId();
        
        log.info("🔍 예약번호로 결제 정보 조회 시작 - 예약번호: {}, 회원ID: {}", reservationId, memberId);
        
        // 1. 예약번호로 결제 정보 조회
        Payment payment = loadPaymentPort.findByReservationId(reservationId)
                .orElseThrow(() -> {
                    log.error("❌ 결제 정보를 찾을 수 없음 - reservationId: {}", reservationId);
                    return new PaymentValidationException("해당 예약번호의 결제 정보를 찾을 수 없습니다");
                });
        
        // 2. 본인 결제인지 확인
        if (payment.getMemberId() == null || !payment.getMemberId().equals(memberId)) {
            throw new PaymentValidationException("본인의 결제 내역만 조회할 수 있습니다");
        }
        
        // 3. 결제 상태 로그 (디버깅용)
        log.info("결제 정보 조회 성공 - 예약번호: {}, 결제상태: {}, 결제일시: {}", 
                reservationId, payment.getPaymentStatus(), payment.getPaidAt());
        
        // 4. 마일리지 거래 내역 조회
        List<MileageTransaction> mileageTransactions = 
                loadMileageTransactionPort.findByPaymentIdOrderByCreatedAtDesc(payment.getId().toString());
        
        // 5. 응답 생성
        return PaymentInfoResponse.builder()
                .paymentId(payment.getId())
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
                        .map(PaymentInfoResponse.MileageTransactionInfo::from)
                        .collect(Collectors.toList()))
                .build();
    }
    
    /**
     * 특정 예약번호로 결제 정보 조회 (비회원/회원 공용)
     * 소유권 검증 없이 조회
     */
    public PaymentInfoResponse getPaymentByReservationIdPublic(Long reservationId) {
        
        log.info("🔍 예약번호로 결제 정보 조회 시작 (공용) - 예약번호: {}", reservationId);
        
        // 1. 예약번호로 결제 정보 조회
        Payment payment = loadPaymentPort.findByReservationId(reservationId)
                .orElseThrow(() -> {
                    log.error("❌ 결제 정보를 찾을 수 없음 - reservationId: {}", reservationId);
                    return new PaymentValidationException("해당 예약번호의 결제 정보를 찾을 수 없습니다");
                });
        
        // 2. 결제 상태 로그 (디버깅용)
        log.info("결제 정보 조회 성공 - 예약번호: {}, 결제상태: {}, 결제일시: {}", 
                reservationId, payment.getPaymentStatus(), payment.getPaidAt());
        
        // 3. 마일리지 거래 내역 조회 (회원인 경우만)
        List<MileageTransaction> mileageTransactions = new ArrayList<>();
        if (payment.getMemberId() != null) {
            mileageTransactions = loadMileageTransactionPort.findByPaymentIdOrderByCreatedAtDesc(payment.getId().toString());
        }
        
        // 4. 응답 생성
        return PaymentInfoResponse.builder()
                .paymentId(payment.getId())
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
                        .map(PaymentInfoResponse.MileageTransactionInfo::from)
                        .collect(Collectors.toList()))
                .build();
    }
    
    /**
     * 특정 결제의 상세 정보 조회 (회원용)
     */
    public PaymentInfoResponse getPaymentDetail(Long paymentId, UserDetails userDetails) {
        
        // UserDetails에서 회원 정보 추출
        Member member = memberRepository.findByMemberNo(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
        Long memberId = member.getId();
        
        log.debug("결제 상세 정보 조회 - 결제ID: {}, 회원ID: {}", paymentId, memberId);
        
        // 1. 결제 정보 조회
        Payment payment = loadPaymentPort.findById(paymentId)
                .orElseThrow(() -> new PaymentValidationException("결제 정보를 찾을 수 없습니다"));
        
        // 2. 본인 결제인지 확인
        if (payment.getMemberId() == null || !payment.getMemberId().equals(memberId)) {
            throw new PaymentValidationException("본인의 결제 내역만 조회할 수 있습니다");
        }
        
        // 3. 마일리지 거래 내역 조회
        List<MileageTransaction> mileageTransactions = 
                loadMileageTransactionPort.findByPaymentIdOrderByCreatedAtDesc(paymentId.toString());
        
        // 4. 응답 생성
        return PaymentInfoResponse.builder()
                .paymentId(payment.getId())
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
                .transactionId(transaction.getId())
                .transactionType(transaction.getType().getDescription())
                .amount(transaction.getPointsAmount())
                .description(transaction.getDescription())
                .processedAt(transaction.getProcessedAt())
                .balanceAfter(transaction.getBalanceAfter())
                .build();
    }
    
    /**
     * 비회원 전체 결제 내역 조회
     * 이름, 전화번호, 비밀번호로 모든 예약 조회
     */
    @Transactional(readOnly = true)
    public PaymentHistoryResponse getAllNonMemberPayments(
            String name, String phoneNumber, String password, Pageable pageable) {
        
        log.info("비회원 전체 결제 내역 조회 시작 - 이름: {}, 전화번호: {}", name, phoneNumber);
        
        // 기본 입력값 검증
        boolean isValid = nonMemberService.validateNonMemberCredentials(name, phoneNumber, password);
        if (!isValid) {
            throw new PaymentNotFoundException("입력하신 정보가 올바르지 않습니다.");
        }
        
        // 비회원의 모든 결제 내역 조회
        Page<Payment> payments = loadPaymentPort.findByNonMemberInfo(name, phoneNumber, pageable);
        
        // 결제 내역이 없는 경우 - 빈 응답 반환
        if (payments.isEmpty()) {
            log.info("비회원 전체 결제 내역 조회 완료 - 조회된 결제 없음");
            return PaymentHistoryResponse.builder()
                    .payments(new ArrayList<>())
                    .currentPage(0)
                    .totalPages(0)
                    .totalElements(0L)
                    .pageSize(pageable.getPageSize())
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();
        }
        
        // 첫 번째 결제 정보로 비밀번호 검증
        Payment firstPayment = payments.getContent().get(0);
        boolean passwordValid = nonMemberService.validateNonMemberInfo(name, phoneNumber, password, firstPayment);
        if (!passwordValid) {
            throw new PaymentNotFoundException("비밀번호가 일치하지 않습니다.");
        }
        
        log.info("비회원 전체 결제 내역 조회 완료 - 조회된 결제 수: {}", payments.getTotalElements());
        
        // 각 결제에 대한 마일리지 거래 내역 및 예약 정보 조회
        List<PaymentHistoryResponse.PaymentHistoryItem> paymentItems = payments.getContent().stream()
                .map(payment -> {
                    List<MileageTransaction> mileageTransactions = 
                            loadMileageTransactionPort.findByPaymentId(payment.getId().toString());
                    
                    return PaymentHistoryResponse.PaymentHistoryItem.from(payment, mileageTransactions);
                })
                .collect(Collectors.toList());
        
        return PaymentHistoryResponse.builder()
                .payments(paymentItems)
                .currentPage(payments.getNumber())
                .totalPages(payments.getTotalPages())
                .totalElements(payments.getTotalElements())
                .pageSize(payments.getSize())
                .hasNext(!payments.isLast())
                .hasPrevious(!payments.isFirst())
                .build();
    }
    
}