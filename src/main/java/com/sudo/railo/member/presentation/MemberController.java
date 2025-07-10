package com.sudo.railo.member.presentation;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sudo.railo.global.security.jwt.TokenExtractor;
import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.member.application.MemberAuthService;
import com.sudo.railo.member.application.MemberService;
import com.sudo.railo.member.application.dto.request.GuestRegisterRequest;
import com.sudo.railo.member.application.dto.request.UpdateEmailRequest;
import com.sudo.railo.member.application.dto.request.UpdatePasswordRequest;
import com.sudo.railo.member.application.dto.request.UpdatePhoneNumberRequest;
import com.sudo.railo.member.application.dto.response.GuestRegisterResponse;
import com.sudo.railo.member.application.dto.response.MemberInfoResponse;
import com.sudo.railo.member.docs.MemberControllerDocs;
import com.sudo.railo.member.success.MemberSuccess;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MemberController implements MemberControllerDocs {

	private final MemberService memberService;
	private final MemberAuthService memberAuthService;
	private final TokenExtractor tokenExtractor;

	@PostMapping("/guest/register")
	public SuccessResponse<GuestRegisterResponse> guestRegister(@RequestBody @Valid GuestRegisterRequest request) {

		GuestRegisterResponse response = memberService.guestRegister(request);

		return SuccessResponse.of(MemberSuccess.GUEST_REGISTER_SUCCESS, response);
	}

	@DeleteMapping("/members")
	public SuccessResponse<?> memberDelete(HttpServletRequest request) {

		String accessToken = tokenExtractor.resolveToken(request);

		memberService.memberDelete(accessToken);

		return SuccessResponse.of(MemberSuccess.MEMBER_DELETE_SUCCESS);
	}

	@GetMapping("/members/me")
	public SuccessResponse<MemberInfoResponse> getMemberInfo() {

		MemberInfoResponse response = memberService.getMemberInfo();

		return SuccessResponse.of(MemberSuccess.MEMBER_INFO_SUCCESS, response);
	}

	@PutMapping("/members/email")
	public SuccessResponse<?> updateEmail(@RequestBody @Valid UpdateEmailRequest request) {

		String newEmail = request.newEmail();
		String authCode = request.authCode();

		memberAuthService.verifyAuthCode(newEmail, authCode);
		memberService.updateEmail(newEmail);

		return SuccessResponse.of(MemberSuccess.MEMBER_EMAIL_UPDATE_SUCCESS);
	}

	@PutMapping("/members/phone-number")
	public SuccessResponse<?> updatePhoneNumber(@RequestBody @Valid UpdatePhoneNumberRequest request) {

		memberService.updatePhoneNumber(request);

		return SuccessResponse.of(MemberSuccess.MEMBER_PHONENUMBER_UPDATE_SUCCESS);
	}

	@PutMapping("/members/password")
	public SuccessResponse<?> updatePassword(@RequestBody @Valid UpdatePasswordRequest request) {

		memberService.updatePassword(request);

		return SuccessResponse.of(MemberSuccess.MEMBER_PASSWORD_UPDATE_SUCCESS);
	}
}
