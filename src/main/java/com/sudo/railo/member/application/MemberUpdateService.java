package com.sudo.railo.member.application;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.auth.application.EmailAuthService;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.redis.MemberRedisRepository;
import com.sudo.railo.auth.application.dto.request.SendCodeRequest;
import com.sudo.railo.member.application.dto.request.UpdateEmailRequest;
import com.sudo.railo.member.application.dto.request.UpdatePasswordRequest;
import com.sudo.railo.member.application.dto.request.UpdatePhoneNumberRequest;
import com.sudo.railo.auth.application.dto.response.SendCodeResponse;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.domain.MemberDetail;
import com.sudo.railo.auth.exception.AuthError;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infrastructure.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberUpdateService {

	private final MemberRepository memberRepository;
	private final EmailAuthService emailAuthService;
	private final PasswordEncoder passwordEncoder;
	private final MemberRedisRepository memberRedisRepository;

	/**
	 * 이메일 인증을 통한 이메일 변경
	 * */
	@Transactional(readOnly = true)
	public SendCodeResponse requestUpdateEmail(SendCodeRequest request, String memberNo) {

		Member member = memberRepository.findByMemberNo(memberNo)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
		MemberDetail memberDetail = member.getMemberDetail();

		String newEmail = request.email();

		// 이미 본인 이메일이랑 동일한 이메일로 변경을 요청했을 경우 예외
		if (memberDetail.getEmail().equals(newEmail)) {
			throw new BusinessException(MemberError.SAME_EMAIL);
		}

		// 다른 회원이 사용중인 이메일을 입력했을 경우 예외
		if (memberRepository.existsByMemberDetailEmail(newEmail)) {
			throw new BusinessException(MemberError.DUPLICATE_EMAIL);
		}

		// 동일 요청 건이 없으면 같은 이메일에 대한 요청이 들어오지 못하도록 redis 에 등록
		if (!memberRedisRepository.handleUpdateEmailRequest(newEmail)) {
			throw new BusinessException(MemberError.EMAIL_UPDATE_ALREADY_REQUESTED);
		}

		return emailAuthService.sendAuthCode(newEmail);
	}

	@Transactional
	public void verifyUpdateEmail(UpdateEmailRequest request, String memberNo) {

		Member member = memberRepository.findByMemberNo(memberNo)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
		MemberDetail memberDetail = member.getMemberDetail();

		String newEmail = request.newEmail();
		String authCode = request.authCode();

		boolean isVerified = emailAuthService.verifyAuthCode(newEmail, authCode); // 이메일 검증

		if (!isVerified) {
			throw new BusinessException(AuthError.INVALID_AUTH_CODE);
		}

		memberDetail.updateEmail(newEmail);
		memberRedisRepository.deleteUpdateEmailRequest(newEmail); // 해당 변경 요청 건 redis 에서 삭제
	}

	/**
	 * 회원 휴대폰 번호 변경
	 * */
	@Transactional
	public void updatePhoneNumber(UpdatePhoneNumberRequest request, String memberNo) {

		Member member = memberRepository.findByMemberNo(memberNo)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));

		// 이미 본인이 사용하는 번호와 동일하게 입력했을 경우 예외
		if (member.getPhoneNumber().equals(request.newPhoneNumber())) {
			throw new BusinessException(MemberError.SAME_PHONE_NUMBER);
		}

		// 다른 회원이 사용 중인 번호를 입력했을 경우 예외
		if (memberRepository.existsByPhoneNumber(request.newPhoneNumber())) {
			throw new BusinessException(MemberError.DUPLICATE_PHONE_NUMBER);
		}

		member.updatePhoneNumber(request.newPhoneNumber());

	}

	/**
	 * 회원 비밀번호 변경
	 * */
	@Transactional
	public void updatePassword(UpdatePasswordRequest request, String memberNo) {

		Member member = memberRepository.findByMemberNo(memberNo)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));

		if (passwordEncoder.matches(request.newPassword(), member.getPassword())) {
			throw new BusinessException(MemberError.SAME_PASSWORD);
		}

		member.updatePassword(passwordEncoder.encode(request.newPassword()));

	}

}
