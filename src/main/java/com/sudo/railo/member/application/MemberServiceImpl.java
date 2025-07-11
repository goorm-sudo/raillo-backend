package com.sudo.railo.member.application;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.redis.RedisUtil;
import com.sudo.railo.global.security.jwt.TokenProvider;
import com.sudo.railo.global.security.util.SecurityUtil;
import com.sudo.railo.member.application.dto.request.FindMemberNoRequest;
import com.sudo.railo.member.application.dto.request.FindPasswordRequest;
import com.sudo.railo.member.application.dto.request.GuestRegisterRequest;
import com.sudo.railo.member.application.dto.request.SendCodeRequest;
import com.sudo.railo.member.application.dto.request.UpdateEmailRequest;
import com.sudo.railo.member.application.dto.request.UpdatePasswordRequest;
import com.sudo.railo.member.application.dto.request.UpdatePhoneNumberRequest;
import com.sudo.railo.member.application.dto.request.VerifyCodeRequest;
import com.sudo.railo.member.application.dto.response.GuestRegisterResponse;
import com.sudo.railo.member.application.dto.response.MemberInfoResponse;
import com.sudo.railo.member.application.dto.response.SendCodeResponse;
import com.sudo.railo.member.application.dto.response.TemporaryTokenResponse;
import com.sudo.railo.member.application.dto.response.VerifyMemberNoResponse;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.domain.MemberDetail;
import com.sudo.railo.member.domain.Role;
import com.sudo.railo.member.exception.AuthError;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infra.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final MemberAuthService memberAuthService;
	private final RedisUtil redisUtil;
	private final TokenProvider tokenProvider;

	@Override
	@Transactional
	public GuestRegisterResponse guestRegister(GuestRegisterRequest request) {

		// 중복 체크
		List<Member> foundMembers = memberRepository.findByNameAndPhoneNumber(request.name(), request.phoneNumber());

		foundMembers.stream()
			.filter(member -> passwordEncoder.matches(request.password(), member.getPassword()))
			.findFirst()
			.ifPresent(member -> {
				throw new BusinessException(MemberError.DUPLICATE_GUEST_INFO);
			});

		String encodedPassword = passwordEncoder.encode(request.password());

		Member member = Member.guestCreate(request.name(), request.phoneNumber(), encodedPassword);
		memberRepository.save(member);

		return new GuestRegisterResponse(request.name(), Role.GUEST);
	}

	// 회원 삭제 로직
	@Override
	@Transactional
	public void memberDelete(String accessToken) {

		String currentMemberNo = SecurityUtil.getCurrentMemberNo();

		Member currentMember = memberRepository.findByMemberNo(currentMemberNo)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));

		try {
			memberRepository.delete(currentMember);
		} catch (Exception e) {
			log.error("회원 삭제 실패 : {}", e.getMessage());
			throw new BusinessException(MemberError.MEMBER_DELETE_FAIL);
		}

		// 로그아웃 수행
		memberAuthService.logout(accessToken);
	}

	// 회원 조회 로직
	@Override
	@Transactional(readOnly = true)
	public MemberInfoResponse getMemberInfo() {

		String currentMemberNo = SecurityUtil.getCurrentMemberNo();

		Member member = memberRepository.findByMemberNo(currentMemberNo)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));

		MemberDetail memberDetail = member.getMemberDetail();

		return MemberInfoResponse.of(member.getName(), member.getPhoneNumber(), memberDetail);
	}

	@Override
	@Transactional(readOnly = true)
	public SendCodeResponse requestUpdateEmail(SendCodeRequest request) {

		Member member = getCurrentMember();
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

		// Redis 에 이메일 변경 요청 상태 저장 시도
		boolean isSuccess = redisUtil.handleUpdateEmailRequest(newEmail);
		if (!isSuccess) {
			throw new BusinessException(MemberError.EMAIL_UPDATE_ALREADY_REQUESTED);
		}

		return memberAuthService.sendAuthCode(newEmail);
	}

	@Override
	@Transactional
	public void verifyUpdateEmail(UpdateEmailRequest request) {

		Member member = getCurrentMember();
		MemberDetail memberDetail = member.getMemberDetail();

		String newEmail = request.newEmail();
		String authCode = request.authCode();

		boolean isVerified = memberAuthService.verifyAuthCode(newEmail, authCode); // 이메일 검증

		if (!isVerified) {
			throw new BusinessException(AuthError.INVALID_AUTH_CODE);
		}

		memberDetail.updateEmail(newEmail);
	}

	@Override
	@Transactional
	public void updatePhoneNumber(UpdatePhoneNumberRequest request) {

		Member member = getCurrentMember();

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

	@Override
	@Transactional
	public void updatePassword(UpdatePasswordRequest request) {

		Member member = getCurrentMember();

		if (passwordEncoder.matches(request.newPassword(), member.getPassword())) {
			throw new BusinessException(MemberError.SAME_PASSWORD);
		}

		member.updatePassword(passwordEncoder.encode(request.newPassword()));

	}

	@Override
	@Transactional(readOnly = true)
	public String getMemberEmail(String memberNo) {
		Member member = memberRepository.findByMemberNo(memberNo)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
		MemberDetail memberDetail = member.getMemberDetail();

		return memberDetail.getEmail();
	}

	/* 회원 번호 찾기 */
	@Override
	@Transactional(readOnly = true)
	public SendCodeResponse requestFindMemberNo(FindMemberNoRequest request) {

		Member member = memberRepository.findMemberByNameAndPhoneNumber(request.name(), request.phoneNumber())
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));

		String memberEmail = member.getMemberDetail().getEmail();
		String memberNo = member.getMemberDetail().getMemberNo();

		sendCodeAndSaveMemberNo(memberEmail, memberNo);

		return new SendCodeResponse(memberEmail);
	}

	@Override
	public VerifyMemberNoResponse verifyFindMemberNo(VerifyCodeRequest request) {

		String memberNo = verifyCodeAndGetMemberNo(request);

		return new VerifyMemberNoResponse(memberNo);
	}

	@Override
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

	@Override
	public TemporaryTokenResponse verifyFindPassword(VerifyCodeRequest request) {

		String memberNo = verifyCodeAndGetMemberNo(request);
		String temporaryToken = tokenProvider.generateTemporaryToken(memberNo); // 5분 동안 유효한 임시토큰 발급

		return new TemporaryTokenResponse(temporaryToken);
	}

	private Member getCurrentMember() {
		String currentMemberNo = SecurityUtil.getCurrentMemberNo();
		return memberRepository.findByMemberNo(currentMemberNo)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
	}

	private String verifyCodeAndGetMemberNo(VerifyCodeRequest request) {

		String email = request.email();
		String authCode = request.authCode();
		boolean isVerified = memberAuthService.verifyAuthCode(email, authCode);

		if (!isVerified) { // 인증 실패 시
			throw new BusinessException(AuthError.INVALID_AUTH_CODE);
		}

		String memberNo = redisUtil.getMemberNo(request.email());
		redisUtil.deleteMemberNo(request.email());

		return memberNo;
	}

	private void sendCodeAndSaveMemberNo(String email, String memberNo) {
		redisUtil.saveMemberNo(email, memberNo); // 레디스에 이메일 검증 후 보낼 회원번호 저장
		memberAuthService.sendAuthCode(email); // 찾아온 이메일로 인증 코드 전송
	}

}
