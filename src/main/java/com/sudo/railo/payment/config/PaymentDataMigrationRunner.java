package com.sudo.railo.payment.config;

import com.sudo.railo.payment.application.service.PaymentDataMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 결제 데이터 마이그레이션 실행
 * 
 * 개발/테스트 환경에서만 자동 실행
 * 운영 환경에서는 수동으로 실행 필요
 */
@Component
@Profile({"dev", "test", "local"})
@RequiredArgsConstructor
@Slf4j
public class PaymentDataMigrationRunner implements ApplicationRunner {
    
    private final PaymentDataMigrationService migrationService;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("======= 결제 데이터 마이그레이션 시작 =======");
        
        try {
            // 마이그레이션 상태 확인
            migrationService.checkMigrationStatus();
            
            // 마이그레이션 실행
            migrationService.migratePaymentTrainInfo();
            
            // 마이그레이션 후 상태 확인
            migrationService.checkMigrationStatus();
            
            log.info("======= 결제 데이터 마이그레이션 완료 =======");
        } catch (Exception e) {
            log.error("결제 데이터 마이그레이션 실패", e);
            // 마이그레이션 실패해도 애플리케이션은 계속 실행
        }
    }
}