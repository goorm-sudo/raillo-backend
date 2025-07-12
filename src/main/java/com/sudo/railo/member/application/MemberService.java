package com.sudo.railo.member.application;

import com.sudo.railo.member.application.dto.request.FindMemberNoRequest;
import com.sudo.railo.member.application.dto.request.FindPasswordRequest;
import com.sudo.railo.member.application.dto.request.GuestRegisterRequest;
import com.sudo.railo.member.application.dto.request.UpdateEmailRequest;
import com.sudo.railo.member.application.dto.request.UpdatePasswordRequest;
import com.sudo.railo.member.application.dto.request.UpdatePhoneNumberRequest;
import com.sudo.railo.member.application.dto.request.VerifyCodeRequest;
import com.sudo.railo.member.application.dto.response.GuestRegisterResponse;
import com.sudo.railo.member.application.dto.response.MemberInfoResponse;
import com.sudo.railo.member.application.dto.response.SendCodeResponse;
import com.sudo.railo.member.application.dto.response.TemporaryTokenResponse;
import com.sudo.railo.member.application.dto.response.VerifyMemberNoResponse;

public interface MemberService {

	GuestRegisterResponse guestRegister(GuestRegisterRequest request);

	void memberDelete(String accessToken);

	MemberInfoResponse getMemberInfo();

	void updateEmail(UpdateEmailRequest request);

	void updatePhoneNumber(UpdatePhoneNumberRequest request);

	void updatePassword(UpdatePasswordRequest request);

	String getMemberEmail(String memberNo);

	// 회원의 현재 마일리지 잔액 조회
	java.math.BigDecimal getMileageBalance(Long memberId);

	SendCodeResponse requestFindMemberNo(FindMemberNoRequest request);

	VerifyMemberNoResponse verifyFindMemberNo(VerifyCodeRequest request);

	SendCodeResponse requestFindPassword(FindPasswordRequest request);

	TemporaryTokenResponse verifyFindPassword(VerifyCodeRequest request);

}
