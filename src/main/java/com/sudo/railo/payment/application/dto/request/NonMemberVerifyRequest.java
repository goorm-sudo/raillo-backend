package com.sudo.railo.payment.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 비회원 결제 확인 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class NonMemberVerifyRequest {
    
    @NotBlank(message = "예약 번호는 필수입니다")
    private String reservationId;
    
    @NotBlank(message = "이름은 필수입니다")
    private String name;
    
    @NotBlank(message = "전화번호는 필수입니다")
    private String phone;
    
    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
} 