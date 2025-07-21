package com.sudo.railo.member.docs;

import com.sudo.railo.global.exception.error.ErrorResponse;
import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.member.application.dto.request.FindMemberNoRequest;
import com.sudo.railo.member.application.dto.request.FindPasswordRequest;
import com.sudo.railo.member.application.dto.request.MemberNoLoginRequest;
import com.sudo.railo.member.application.dto.request.SendCodeRequest;
import com.sudo.railo.member.application.dto.request.SignUpRequest;
import com.sudo.railo.member.application.dto.request.UpdateEmailRequest;
import com.sudo.railo.member.application.dto.request.VerifyCodeRequest;
import com.sudo.railo.member.application.dto.response.ReissueTokenResponse;
import com.sudo.railo.member.application.dto.response.SendCodeResponse;
import com.sudo.railo.member.application.dto.response.SignUpResponse;
import com.sudo.railo.member.application.dto.response.TemporaryTokenResponse;
import com.sudo.railo.member.application.dto.response.TokenResponse;
import com.sudo.railo.member.application.dto.response.VerifyCodeResponse;
import com.sudo.railo.member.application.dto.response.VerifyMemberNoResponse;

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
		@ApiResponse(responseCode = "401", description = "이미 로그아웃된 토큰입니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<?> logout(HttpServletRequest request, String memberNo);

	@Operation(method = "POST", summary = "accessToken 재발급", description = "accessToken 이 만료되었을 때, 토큰을 재발급 받을 수 있도록 합니다.",
		security = {@SecurityRequirement(name = "bearerAuth")})
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "accessToken 이 성공적으로 재발급되었습니다."),
		@ApiResponse(responseCode = "401", description = "유효하지 않은 토큰입니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<ReissueTokenResponse> reissue(HttpServletRequest request, String memberNo);

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

	@Operation(method = "POST", summary = "회원번호 찾기 요청", description = "회원번호를 찾기 위한 요청을 받고, 본인인증을 위한 이메일 인증 코드를 전송합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "이메일 인증 코드 전송을 성공했습니다."),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<SendCodeResponse> requestFindMemberNo(FindMemberNoRequest request);

	@Operation(method = "POST", summary = "회원번호 찾기 인증코드 검증 요청", description = "회원번호를 찾기 위해 인증코드 검증 후, 성공하면 회원번호를 응답으로 보냅니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "이메일 인증 코드 검증에 성공했습니다."),
		@ApiResponse(responseCode = "401", description = "인증 코드가 일치하지 않습니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<VerifyMemberNoResponse> verifyFindMemberNo(VerifyCodeRequest request);

	@Operation(method = "POST", summary = "비밀번호 찾기 요청", description = "비밀번호를 찾기 위한 요청을 받고, 본인인증을 위한 이메일 인증 코드를 전송합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "이메일 인증 코드 전송을 성공했습니다."),
		@ApiResponse(responseCode = "400", description = "이름이 일치하지 않습니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<SendCodeResponse> requestFindPassword(FindPasswordRequest request);

	@Operation(method = "POST", summary = "비밀번호 찾기 인증코드 검증 요청", description = "비밀번호를 찾기 위해 인증코드 검증 후, 성공하면 유효시간이 5분인 임시토큰을 응답으로 보냅니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "이메일 인증 코드 검증에 성공했습니다."),
		@ApiResponse(responseCode = "401", description = "인증 코드가 일치하지 않습니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<TemporaryTokenResponse> verifyFindPassword(VerifyCodeRequest request);

	@Operation(method = "POST", summary = "이메일 변경 요청", description = "요청으로 변경할 이메일을 받아 db 내 정보로 변경 가능 여부 확인 후 이메일 인증 코드를 보냅니다.",
		security = {@SecurityRequirement(name = "bearerAuth")})
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "이메일 인증 코드 전송을 성공했습니다."),
		@ApiResponse(responseCode = "409", description = "현재 사용하는 이메일과 동일합니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(responseCode = "409", description = "이미 사용중인 이메일입니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<SendCodeResponse> requestUpdateEmail(SendCodeRequest request, String memberNo);

	@Operation(method = "PUT", summary = "이메일 변경 인증코드 검증 요청", description = "이메일 변경 전 사용 가능한 이메일인지 검증 후, 회원 이메일을 변경합니다.",
		security = {@SecurityRequirement(name = "bearerAuth")})
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "이메일 변경에 성공했습니다."),
		@ApiResponse(responseCode = "401", description = "인증 코드가 일치하지 않습니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<?> verifyUpdateEmail(UpdateEmailRequest request, String memberNo);
}
