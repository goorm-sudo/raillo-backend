package com.sudo.railo.member.docs;

import com.sudo.railo.global.exception.error.ErrorResponse;
import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.member.application.dto.request.MemberNoLoginRequest;
import com.sudo.railo.member.application.dto.request.SignUpRequest;
import com.sudo.railo.member.application.dto.response.ReissueTokenResponse;
import com.sudo.railo.member.application.dto.response.SignUpResponse;
import com.sudo.railo.member.application.dto.response.TokenResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Authentication", description = "🔐 인증 API - 회원 로그인, 회원가입, 토큰 관리 API")
public interface AuthControllerDocs {

	@Operation(method = "POST", summary = "회원가입", description = "사용자 정보를 받아 회원가입을 수행합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "회원가입에 성공하였습니다."),
		@ApiResponse(responseCode = "409", description = "이미 사용중인 이메일입니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(responseCode = "400", description = "요청 본문이 유효하지 않습니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<SignUpResponse> signUp(SignUpRequest request);

	@Operation(method = "POST", summary = "회원번호 로그인", description = "회원번호와 비밀번호를 받아 로그인을 수행합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "로그인에 성공하였습니다."),
		@ApiResponse(responseCode = "401", description = "비밀번호가 일치하지 않습니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<TokenResponse> memberNoLogin(MemberNoLoginRequest request);

	@Operation(method = "POST", summary = "로그아웃", description = "로그인 되어있는 회원을 로그아웃 처리합니다.",
		security = {@SecurityRequirement(name = "bearerAuth")})
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "로그아웃에 성공하였습니다."),
		@ApiResponse(responseCode = "403", description = "이미 로그아웃된 토큰입니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<?> logout(HttpServletRequest request);

	@Operation(method = "POST", summary = "accessToken 재발급", description = "accessToken 이 만료되었을 때, 토큰을 재발급 받을 수 있도록 합니다.",
		security = {@SecurityRequirement(name = "bearerAuth")})
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "accessToken 이 성공적으로 재발급되었습니다."),
		@ApiResponse(responseCode = "403", description = "유효하지 않은 토큰입니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<ReissueTokenResponse> reissue(HttpServletRequest request);
}
