package com.sudo.railo.member.application;

import com.sudo.railo.member.application.dto.request.GuestRegisterRequest;
import com.sudo.railo.member.application.dto.response.GuestRegisterResponse;

public interface MemberService {

	GuestRegisterResponse guestRegister(GuestRegisterRequest request);
}
