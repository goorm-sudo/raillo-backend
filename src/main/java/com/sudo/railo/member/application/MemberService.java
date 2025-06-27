package com.sudo.railo.member.application;

import com.sudo.railo.member.application.dto.request.GuestRegisterRequest;
import com.sudo.railo.member.application.dto.response.GuestRegisterResponse;
import com.sudo.railo.member.application.dto.response.MemberInfoResponse;

import jakarta.servlet.http.HttpServletRequest;

public interface MemberService {

	GuestRegisterResponse guestRegister(GuestRegisterRequest request);

	void memberDelete(HttpServletRequest request);

	MemberInfoResponse getMemberInfo();
}
