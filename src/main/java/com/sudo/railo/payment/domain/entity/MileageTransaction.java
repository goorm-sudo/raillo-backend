package com.sudo.railo.payment.domain.entity;

import com.sudo.railo.global.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 마일리지 거래 내역 엔티티
 * 적립, 사용, 만료, 조정 등 모든 마일리지 변동 내역을 기록
 */
@Entity
@Table(name = "MileageTransactions")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class MileageTransaction extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;
    
    @Column(name = "member_id", nullable = false)
    private Long memberId;
    
    @Column(name = "payment_id")
    private String paymentId;           // 결제와 연결 (적립/사용 시)
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType type;       // EARN, USE, EXPIRE, ADJUST
    
    @Column(name = "points_amount", precision = 10, scale = 0, nullable = false)
    private BigDecimal pointsAmount;    // 포인트 수량 (양수: 적립, 음수: 차감)
    
    @Column(name = "balance_before", precision = 10, scale = 0, nullable = false)
    private BigDecimal balanceBefore;   // 거래 전 잔액
    
    @Column(name = "balance_after", precision = 10, scale = 0, nullable = false)
    private BigDecimal balanceAfter;    // 거래 후 잔액
    
    @Column(name = "description", length = 500)
    private String description;         // 거래 설명
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;    // 적립 포인트 만료일 (적립 시에만)
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;   // PENDING, COMPLETED, CANCELLED
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;  // 처리 완료 시간
    
    /**
     * 마일리지 거래 유형
     */
    public enum TransactionType {
        EARN("적립"),           // 구매 시 적립
        USE("사용"),            // 결제 시 사용
        EXPIRE("만료"),         // 유효기간 만료
        ADJUST("조정"),         // 관리자 조정
        REFUND("환불");         // 환불로 인한 복구
        
        private final String description;
        
        TransactionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 거래 상태
     */
    public enum TransactionStatus {
        PENDING("처리 대기"),       // 거래 대기 중
        COMPLETED("완료"),         // 거래 완료
        CANCELLED("취소"),         // 거래 취소
        FAILED("실패");           // 거래 실패
        
        private final String description;
        
        TransactionStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 거래 완료 처리
     */
    public void complete() {
        this.status = TransactionStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * 거래 취소 처리
     */
    public void cancel() {
        this.status = TransactionStatus.CANCELLED;
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * 적립 거래 생성 팩토리 메서드
     */
    public static MileageTransaction createEarnTransaction(
            Long memberId, 
            String paymentId, 
            BigDecimal pointsAmount, 
            BigDecimal balanceBefore,
            String description) {
        
        return MileageTransaction.builder()
                .memberId(memberId)
                .paymentId(paymentId)
                .type(TransactionType.EARN)
                .pointsAmount(pointsAmount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceBefore.add(pointsAmount))
                .description(description)
                .expiresAt(LocalDateTime.now().plusYears(1)) // 1년 후 만료
                .status(TransactionStatus.PENDING)
                .build();
    }
    
    /**
     * 사용 거래 생성 팩토리 메서드
     */
    public static MileageTransaction createUseTransaction(
            Long memberId, 
            String paymentId, 
            BigDecimal pointsAmount, 
            BigDecimal balanceBefore,
            String description) {
        
        return MileageTransaction.builder()
                .memberId(memberId)
                .paymentId(paymentId)
                .type(TransactionType.USE)
                .pointsAmount(pointsAmount.negate()) // 음수로 저장
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceBefore.subtract(pointsAmount))
                .description(description)
                .status(TransactionStatus.PENDING)
                .build();
    }
} 