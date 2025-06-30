package com.sudo.railo.member.application;

import com.sudo.railo.member.application.dto.request.GuestRegisterRequest;
import com.sudo.railo.member.application.dto.response.GuestRegisterResponse;
import com.sudo.railo.member.application.dto.response.MemberInfoResponse;

public interface MemberService {

	GuestRegisterResponse guestRegister(GuestRegisterRequest request);

	void memberDelete(String accessToken);

	MemberInfoResponse getMemberInfo();
}
