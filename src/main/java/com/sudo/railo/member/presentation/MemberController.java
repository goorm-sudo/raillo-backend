package com.sudo.railo.member.presentation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.member.application.MemberService;
import com.sudo.railo.member.application.dto.request.GuestRegisterRequest;
import com.sudo.railo.member.application.dto.response.GuestRegisterResponse;
import com.sudo.railo.member.success.MemberSuccess;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	@PostMapping("/guest/register")
	public SuccessResponse<GuestRegisterResponse> guestRegister(@RequestBody @Valid GuestRegisterRequest request) {

		GuestRegisterResponse response = memberService.guestRegister(request);

		return SuccessResponse.of(MemberSuccess.GUEST_REGISTER_SUCCESS, response);
	}
}
