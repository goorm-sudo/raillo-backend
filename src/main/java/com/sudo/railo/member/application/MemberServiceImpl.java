package com.sudo.railo.member.application;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.security.util.SecurityUtil;
import com.sudo.railo.member.application.dto.request.GuestRegisterRequest;
import com.sudo.railo.member.application.dto.request.UpdateEmailRequest;
import com.sudo.railo.member.application.dto.request.UpdatePasswordRequest;
import com.sudo.railo.member.application.dto.request.UpdatePhoneNumberRequest;
import com.sudo.railo.member.application.dto.response.GuestRegisterResponse;
import com.sudo.railo.member.application.dto.response.MemberInfoResponse;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.domain.MemberDetail;
import com.sudo.railo.member.domain.Role;
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
	@Transactional
	public void updatedEmail(UpdateEmailRequest request) {

		String currentMemberNo = SecurityUtil.getCurrentMemberNo();

		Member member = memberRepository.findByMemberNo(currentMemberNo)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));

		MemberDetail memberDetail = member.getMemberDetail();

		// 이미 본인 이메일이랑 동일한 이메일로 변경을 요청했을 경우 예외
		if (memberDetail.getEmail().equals(request.newEmail())) {
			throw new BusinessException(MemberError.SAME_EMAIL);
		}

		// 다른 회원이 사용중인 이메일을 입력했을 경우 예외
		if (memberRepository.existsByMemberDetailEmail(request.newEmail())) {
			throw new BusinessException(MemberError.DUPLICATE_EMAIL);
		}

		memberDetail.updateEmail(request.newEmail());
		memberRepository.save(member);
	}

	@Override
	@Transactional
	public void updatedPhoneNumber(UpdatePhoneNumberRequest request) {

		String currentMemberNo = SecurityUtil.getCurrentMemberNo();

		Member member = memberRepository.findByMemberNo(currentMemberNo)
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
		memberRepository.save(member);
	}

	@Override
	@Transactional
	public void updatedPassword(UpdatePasswordRequest request) {

		String currentMemberNo = SecurityUtil.getCurrentMemberNo();

		Member member = memberRepository.findByMemberNo(currentMemberNo)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));

		if (passwordEncoder.matches(request.newPassword(), member.getPassword())) {
			throw new BusinessException(MemberError.SAME_PASSWORD);
		}

		member.updatePassword(passwordEncoder.encode(request.newPassword()));
		memberRepository.save(member);
	}
}
