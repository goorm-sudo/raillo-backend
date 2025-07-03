package com.sudo.railo.member.application;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.application.dto.request.UpdateEmailRequest;
import com.sudo.railo.member.application.dto.request.UpdatePasswordRequest;
import com.sudo.railo.member.application.dto.request.UpdatePhoneNumberRequest;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.domain.MemberDetail;
import com.sudo.railo.member.domain.Membership;
import com.sudo.railo.member.domain.Role;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infra.MemberRepository;

@SpringBootTest
@Transactional
class MemberServiceImplTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private MemberService memberService;

	private Member testMember;

	@BeforeEach
	void setUp() {
		MemberDetail memberDetail = MemberDetail.create(
			"202507020001",
			Membership.BUSINESS,
			"test01@email.com",
			LocalDate.of(1990, 1, 1),
			"M"
		);

		testMember = Member.create(
			"홍길동",
			"01012341234",
			passwordEncoder.encode("testPwd"),
			Role.MEMBER,
			memberDetail
		);
		memberRepository.save(testMember);

		MemberDetail anotherMemberDetail = MemberDetail.create(
			"202507020002",
			Membership.BUSINESS,
			"test02@email.com",
			LocalDate.of(1990, 2, 2),
			"M"
		);

		Member anotherMember = Member.create(
			"유관순",
			"01012345678",
			passwordEncoder.encode("anotherPwd"),
			Role.MEMBER,
			anotherMemberDetail
		);
		memberRepository.save(anotherMember);

		// 서비스 계층에서 SecurityUtil 을 사용하고 있기 때문에 직접 SecurityContext 를 set
		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken("202507020001", "testPwd", List.of(() -> "MEMBER"));
		SecurityContextHolder.getContext().setAuthentication(authentication);

	}

	@DisplayName("로그인 된 사용자의 이메일 변경 성공")
	@Test
	void updateEmailSuccess() {

		//given
		UpdateEmailRequest request = new UpdateEmailRequest("updateMail@email.com");

		//when
		memberService.updateEmail(request);

		//then
		Member result = memberRepository.findByMemberNo(testMember.getMemberDetail().getMemberNo())
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
		assertThat(result.getMemberDetail().getEmail()).isEqualTo(request.newEmail());
	}

	@DisplayName("로그인 된 사용자의 휴대폰 번호 변경 성공")
	@Test
	void updatePhoneNumberSuccess() {

		//given
		UpdatePhoneNumberRequest request = new UpdatePhoneNumberRequest("01012341111");

		//when
		memberService.updatePhoneNumber(request);

		//then
		Member result = memberRepository.findByMemberNo(testMember.getMemberDetail().getMemberNo())
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
		assertThat(result.getPhoneNumber()).isEqualTo(request.newPhoneNumber());
	}

	@DisplayName("로그인 된 사용자의 비밀번호 변경 성공")
	@Test
	void updatePasswordSuccess() {

		//given
		UpdatePasswordRequest request = new UpdatePasswordRequest("updatePwd");

		//when
		memberService.updatePassword(request);

		//then
		Member result = memberRepository.findByMemberNo(testMember.getMemberDetail().getMemberNo())
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
		assertThat(passwordEncoder.matches(request.newPassword(), result.getPassword())).isTrue();
	}

}
