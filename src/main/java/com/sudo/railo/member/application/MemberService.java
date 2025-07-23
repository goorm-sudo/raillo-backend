package com.sudo.railo.member.application;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.application.dto.request.GuestRegisterRequest;
import com.sudo.railo.member.application.dto.response.GuestRegisterResponse;
import com.sudo.railo.member.application.dto.response.MemberInfoResponse;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.domain.MemberDetail;
import com.sudo.railo.member.domain.Role;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infrastructure.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthService authService;

	/**
	 * 비회원 등록
	 * */
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

	/**
	 * 회원 삭제
	 * */
	@Transactional
	public void memberDelete(String accessToken, String memberNo) {

		Member currentMember = memberRepository.findByMemberNo(memberNo)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));

		try {
			memberRepository.delete(currentMember);
		} catch (Exception e) {
			log.error("회원 삭제 실패 : {}", e.getMessage());
			throw new BusinessException(MemberError.MEMBER_DELETE_FAIL);
		}

		// 로그아웃 수행
		authService.logout(accessToken, memberNo);
	}

	/**
	 * 회원 정보 조회
	 * */
	@Transactional(readOnly = true)
	public MemberInfoResponse getMemberInfo(String memberNo) {

		Member member = memberRepository.findByMemberNo(memberNo)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));

		MemberDetail memberDetail = member.getMemberDetail();

		return MemberInfoResponse.of(member.getName(), member.getPhoneNumber(), memberDetail);
	}

	/**
	 * 회원 이메일 조회
	 * */
	@Transactional(readOnly = true)
	public String getMemberEmail(String memberNo) {
		Member member = memberRepository.findByMemberNo(memberNo)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
		MemberDetail memberDetail = member.getMemberDetail();

		return memberDetail.getEmail();
	}

}
