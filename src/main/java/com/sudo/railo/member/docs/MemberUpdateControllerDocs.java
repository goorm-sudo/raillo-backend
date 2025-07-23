package com.sudo.railo.member.docs;

import com.sudo.railo.global.exception.error.ErrorResponse;
import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.auth.application.dto.request.SendCodeRequest;
import com.sudo.railo.member.application.dto.request.UpdateEmailRequest;
import com.sudo.railo.member.application.dto.request.UpdatePasswordRequest;
import com.sudo.railo.member.application.dto.request.UpdatePhoneNumberRequest;
import com.sudo.railo.auth.application.dto.response.SendCodeResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

public interface MemberUpdateControllerDocs {

	@Operation(method = "POST", summary = "이메일 변경 요청", description = "요청으로 변경할 이메일을 받아 db 내 정보로 변경 가능 여부 확인 후 이메일 인증 코드를 보냅니다.",
		tags = {"AuthMembers"},
		security = {@SecurityRequirement(name = "bearerAuth")})
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "이메일 인증 코드 전송을 성공했습니다."),
		@ApiResponse(responseCode = "409", description = "현재 사용하는 이메일과 동일합니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(responseCode = "409", description = "이미 사용중인 이메일입니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<SendCodeResponse> requestUpdateEmail(SendCodeRequest request, String memberNo);

	@Operation(method = "PUT", summary = "이메일 변경 인증코드 검증 요청", description = "이메일 변경 전 사용 가능한 이메일인지 검증 후, 회원 이메일을 변경합니다.",
		tags = {"AuthMembers"},
		security = {@SecurityRequirement(name = "bearerAuth")})
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "이메일 변경에 성공했습니다."),
		@ApiResponse(responseCode = "401", description = "인증 코드가 일치하지 않습니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<?> verifyUpdateEmail(UpdateEmailRequest request, String memberNo);

	@Operation(method = "PUT", summary = "휴대폰 번호 변경", description = "요청으로 변경할 휴대폰 번호를 받아 회원 정보의 휴대폰 번호를 새로운 번호로 변경합니다.",
		tags = {"Members"},
		security = {@SecurityRequirement(name = "bearerAuth")})
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "휴대폰 번호 변경에 성공했습니다."),
		@ApiResponse(responseCode = "409", description = "현재 사용하는 휴대폰 번호와 동일합니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(responseCode = "409", description = "이미 사용중인 이메일입니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<?> updatePhoneNumber(UpdatePhoneNumberRequest request, String memberNo);

	@Operation(method = "PUT", summary = "비밀번호 변경", description = "요청으로 변경할 비밀번호를 받아 회원 정보의 비밀번호를 새로운 비밀번호로 변경합니다.",
		tags = {"Members"},
		security = {@SecurityRequirement(name = "bearerAuth")})
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "비밀번호 변경에 성공했습니다."),
		@ApiResponse(responseCode = "409", description = "현재 사용하는 비밀번호와 동일합니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<?> updatePassword(UpdatePasswordRequest request, String memberNo);

}
