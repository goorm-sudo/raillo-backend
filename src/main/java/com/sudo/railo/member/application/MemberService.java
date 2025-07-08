package com.sudo.railo.member.application;

import com.sudo.railo.member.application.dto.request.FindMemberNoRequest;
import com.sudo.railo.member.application.dto.request.GuestRegisterRequest;
import com.sudo.railo.member.application.dto.request.UpdateEmailRequest;
import com.sudo.railo.member.application.dto.request.UpdatePasswordRequest;
import com.sudo.railo.member.application.dto.request.UpdatePhoneNumberRequest;
import com.sudo.railo.member.application.dto.request.VerifyCodeRequest;
import com.sudo.railo.member.application.dto.response.GuestRegisterResponse;
import com.sudo.railo.member.application.dto.response.MemberInfoResponse;
import com.sudo.railo.member.application.dto.response.SendCodeResponse;
import com.sudo.railo.member.application.dto.response.VerifyMemberNoResponse;

public interface MemberService {

	GuestRegisterResponse guestRegister(GuestRegisterRequest request);

	void memberDelete(String accessToken);

	MemberInfoResponse getMemberInfo();

	void updateEmail(UpdateEmailRequest request);

	void updatePhoneNumber(UpdatePhoneNumberRequest request);

	void updatePassword(UpdatePasswordRequest request);

	String getMemberEmail(String memberNo);

	SendCodeResponse requestFindMemberNo(FindMemberNoRequest request);

	VerifyMemberNoResponse verifyFindMemberNo(VerifyCodeRequest request);

}
