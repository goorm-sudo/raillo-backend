package com.sudo.railo.member.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.auth.application.EmailAuthService;
import com.sudo.railo.auth.application.dto.request.VerifyCodeRequest;
import com.sudo.railo.auth.application.dto.response.SendCodeResponse;
import com.sudo.railo.auth.application.dto.response.TemporaryTokenResponse;
import com.sudo.railo.auth.exception.AuthError;
import com.sudo.railo.auth.security.jwt.TokenGenerator;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.redis.MemberRedisRepository;
import com.sudo.railo.member.application.dto.request.FindMemberNoRequest;
import com.sudo.railo.member.application.dto.request.FindPasswordRequest;
import com.sudo.railo.member.application.dto.response.VerifyMemberNoResponse;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infrastructure.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberFindService {

	private final MemberRepository memberRepository;
	private final TokenGenerator tokenGenerator;
	private final EmailAuthService emailAuthService;
	private final MemberRedisRepository memberRedisRepository;

	/**
	 * 이메일 인증을 통한 회원 번호 찾기
	 * */
	@Transactional(readOnly = true)
	public SendCodeResponse requestFindMemberNo(FindMemberNoRequest request) {

		Member member = memberRepository.findMemberByNameAndPhoneNumber(request.name(), request.phoneNumber())
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));

		String memberEmail = member.getMemberDetail().getEmail();
		String memberNo = member.getMemberDetail().getMemberNo();

		sendCodeAndSaveMemberNo(memberEmail, memberNo);

		return new SendCodeResponse(memberEmail);
	}

	public VerifyMemberNoResponse verifyFindMemberNo(VerifyCodeRequest request) {

		String memberNo = verifyCodeAndGetMemberNo(request);

		return new VerifyMemberNoResponse(memberNo);
	}

	/**
	 * 이메일 인증을 통한 비밀번호 찾기
	 * */
	@Transactional(readOnly = true)
	public SendCodeResponse requestFindPassword(FindPasswordRequest request) {

		Member member = memberRepository.findByMemberNo(request.memberNo())
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));

		if (!member.getName().equals(request.name())) {
			throw new BusinessException(MemberError.NAME_MISMATCH);
		}

		String memberEmail = member.getMemberDetail().getEmail();
		String memberNo = member.getMemberDetail().getMemberNo();

		sendCodeAndSaveMemberNo(memberEmail, memberNo);

		return new SendCodeResponse(memberEmail);
	}

	public TemporaryTokenResponse verifyFindPassword(VerifyCodeRequest request) {

		String memberNo = verifyCodeAndGetMemberNo(request);
		String temporaryToken = tokenGenerator.generateTemporaryToken(memberNo);

		return new TemporaryTokenResponse(temporaryToken);
	}

	private String verifyCodeAndGetMemberNo(VerifyCodeRequest request) {

		String email = request.email();
		String authCode = request.authCode();
		boolean isVerified = emailAuthService.verifyAuthCode(email, authCode);

		if (!isVerified) { // 인증 실패 시
			throw new BusinessException(AuthError.INVALID_AUTH_CODE);
		}

		String memberNo = memberRedisRepository.getMemberNo(request.email());
		memberRedisRepository.deleteMemberNo(request.email());

		return memberNo;
	}

	private void sendCodeAndSaveMemberNo(String email, String memberNo) {
		memberRedisRepository.saveMemberNo(email, memberNo); // 레디스에 이메일 검증 후 보낼 회원번호 저장
		emailAuthService.sendAuthCode(email); // 찾아온 이메일로 인증 코드 전송
	}

}
