package com.sudo.railo.member.presentation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sudo.railo.auth.security.jwt.TokenExtractor;
import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.member.application.MemberService;
import com.sudo.railo.member.application.dto.request.GuestRegisterRequest;
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
	private final TokenExtractor tokenExtractor;

	@PostMapping("/guest/register")
	public SuccessResponse<GuestRegisterResponse> guestRegister(@RequestBody @Valid GuestRegisterRequest request) {

		GuestRegisterResponse response = memberService.guestRegister(request);

		return SuccessResponse.of(MemberSuccess.GUEST_REGISTER_SUCCESS, response);
	}

	@DeleteMapping("/members")
	public SuccessResponse<?> memberDelete(HttpServletRequest request,
		@AuthenticationPrincipal(expression = "username") String memberNo) {

		String accessToken = tokenExtractor.resolveToken(request);

		memberService.memberDelete(accessToken, memberNo);

		return SuccessResponse.of(MemberSuccess.MEMBER_DELETE_SUCCESS);
	}

	@GetMapping("/members/me")
	public SuccessResponse<MemberInfoResponse> getMemberInfo(
		@AuthenticationPrincipal(expression = "username") String memberNo) {

		MemberInfoResponse response = memberService.getMemberInfo(memberNo);

		return SuccessResponse.of(MemberSuccess.MEMBER_INFO_SUCCESS, response);
	}

}
