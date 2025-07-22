package com.sudo.railo.member.docs;

import com.sudo.railo.global.exception.error.ErrorResponse;
import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.member.application.dto.request.GuestRegisterRequest;
import com.sudo.railo.member.application.dto.request.UpdatePasswordRequest;
import com.sudo.railo.member.application.dto.request.UpdatePhoneNumberRequest;
import com.sudo.railo.member.application.dto.response.GuestRegisterResponse;
import com.sudo.railo.member.application.dto.response.MemberInfoResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Members", description = "👤 회원 API - 회원 정보 조회, 수정, 탈퇴, 관리 API")
public interface MemberControllerDocs {

	@Operation(method = "POST", summary = "비회원 등록", description = "비회원 정보를 등록합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "비회원 정보 등록이 성공적으로 완료되었습니다."),
		@ApiResponse(responseCode = "409", description = "이미 동일한 비회원 정보가 존재합니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<GuestRegisterResponse> guestRegister(GuestRegisterRequest request);

	@Operation(method = "DELETE", summary = "회원 삭제", description = "로그인 되어 있는 회원을 삭제하여 탈퇴처리 합니다.",
		security = {@SecurityRequirement(name = "bearerAuth")})
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "회원 탈퇴가 성공적으로 완료되었습니다."),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<?> memberDelete(HttpServletRequest request, String memberNo);

	@Operation(method = "GET", summary = "단일 회원 정보 조회", description = "로그인 되어 있는 회원의 정보를 조회합니다.",
		security = {@SecurityRequirement(name = "bearerAuth")})
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "회원 정보 조회에 성공했습니다."),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<MemberInfoResponse> getMemberInfo(String memberNo);

	@Operation(method = "PUT", summary = "휴대폰 번호 변경", description = "요청으로 변경할 휴대폰 번호를 받아 회원 정보의 휴대폰 번호를 새로운 번호로 변경합니다.",
		security = {@SecurityRequirement(name = "bearerAuth")})
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "휴대폰 번호 변경에 성공했습니다."),
		@ApiResponse(responseCode = "409", description = "현재 사용하는 휴대폰 번호와 동일합니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(responseCode = "409", description = "이미 사용중인 이메일입니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<?> updatePhoneNumber(UpdatePhoneNumberRequest request, String memberNo);

	@Operation(method = "PUT", summary = "비밀번호 변경", description = "요청으로 변경할 비밀번호를 받아 회원 정보의 비밀번호를 새로운 비밀번호로 변경합니다.",
		security = {@SecurityRequirement(name = "bearerAuth")})
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "비밀번호 변경에 성공했습니다."),
		@ApiResponse(responseCode = "409", description = "현재 사용하는 비밀번호와 동일합니다.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	SuccessResponse<?> updatePassword(UpdatePasswordRequest request, String memberNo);

}
