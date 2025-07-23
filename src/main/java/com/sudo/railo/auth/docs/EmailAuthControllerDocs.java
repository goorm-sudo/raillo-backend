package com.sudo.railo.auth.docs;

import com.sudo.railo.global.exception.error.ErrorResponse;
import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.auth.application.dto.request.SendCodeRequest;
import com.sudo.railo.auth.application.dto.request.VerifyCodeRequest;
import com.sudo.railo.auth.application.dto.response.SendCodeResponse;
import com.sudo.railo.auth.application.dto.response.VerifyCodeResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Authentication", description = "🔐 인증 API - 회원 로그인, 회원가입, 토큰 관리 API")
public interface EmailAuthControllerDocs {

	@Operation(method = "POST", summary = "인증되지 않은 사용자용 이메일 인증코드 전송 요청", description = "회원번호 찾기, 비밀번호 찾기 등 로그인 할 수 없는 상황에서 사용되는 이메일 인증 요청입니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "이메일 인증 코드 전송을 성공했습니다."),
		@ApiResponse(responseCode = "400", description = "요청 본문이 유효하지 않습니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<SendCodeResponse> sendAuthCode(SendCodeRequest request);

	@Operation(method = "POST", summary = "인증된 사용자용 이메일 인증코드 전송 요청", description = "이메일 변경, 휴대폰 번호 변경 등 로그인 되어 있는 상태에서 사용되는 이메일 인증 요청입니다.",
		security = {@SecurityRequirement(name = "bearerAuth")})
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "이메일 인증 코드 전송을 성공했습니다."),
		@ApiResponse(responseCode = "400", description = "요청 본문이 유효하지 않습니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<?> sendAuthCodeWithMember(String memberNo);

	@Operation(method = "POST", summary = "이메일 인증 코드 검증", description = "인증된 사용자와 인증되지 않은 사용자 모두 이메일 인증 코드 검증 시 사용합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "인증 코드 검증에 성공했을 경우 true, 실패했을 경우 false"),
		@ApiResponse(responseCode = "400", description = "요청 본문이 유효하지 않습니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<VerifyCodeResponse> verifyAuthCode(VerifyCodeRequest request);
}
