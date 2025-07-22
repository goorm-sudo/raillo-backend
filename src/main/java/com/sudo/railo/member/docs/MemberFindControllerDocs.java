package com.sudo.railo.member.docs;

import com.sudo.railo.global.exception.error.ErrorResponse;
import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.member.application.dto.request.FindMemberNoRequest;
import com.sudo.railo.member.application.dto.request.FindPasswordRequest;
import com.sudo.railo.member.application.dto.request.VerifyCodeRequest;
import com.sudo.railo.member.application.dto.response.SendCodeResponse;
import com.sudo.railo.member.application.dto.response.TemporaryTokenResponse;
import com.sudo.railo.member.application.dto.response.VerifyMemberNoResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "AuthMembers", description = "인증+회원 api - 이메일 인증을 통한 회원 정보 찾기 및 변경")
public interface MemberFindControllerDocs {

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
}
