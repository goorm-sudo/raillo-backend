package com.sudo.railo.payment.infrastructure.persistence;

import com.sudo.railo.payment.domain.entity.MileageTransaction;
import com.sudo.railo.payment.domain.repository.MileageTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MileageTransaction JPA Repository 구현체
 */
@Repository
public interface JpaMileageTransactionRepository 
        extends JpaRepository<MileageTransaction, Long>, MileageTransactionRepository {
    
    // MileageTransactionRepository의 모든 메서드는 JPA가 자동 구현
    // 추가적인 JPA 특화 메서드가 필요한 경우 여기에 정의
    
    /**
     * 회원의 활성 마일리지 잔액 조회 (만료되지 않은 적립분만)
     */
    @Query("SELECT COALESCE(SUM(mt.pointsAmount), 0) FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.status = 'COMPLETED' " +
           "AND (mt.expiresAt IS NULL OR mt.expiresAt > :currentTime)")
    BigDecimal calculateActiveBalance(
            @Param("memberId") Long memberId, 
            @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 회원의 총 적립 포인트 (전체 기간)
     */
    @Query("SELECT COALESCE(SUM(mt.pointsAmount), 0) FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.type = 'EARN' " +
           "AND mt.status = 'COMPLETED'")
    BigDecimal calculateTotalEarned(@Param("memberId") Long memberId);
    
    /**
     * 회원의 총 사용 포인트 (전체 기간)
     */
    @Query("SELECT COALESCE(SUM(ABS(mt.pointsAmount)), 0) FROM MileageTransaction mt " +
           "WHERE mt.memberId = :memberId " +
           "AND mt.type = 'USE' " +
           "AND mt.status = 'COMPLETED'")
    BigDecimal calculateTotalUsed(@Param("memberId") Long memberId);
} 