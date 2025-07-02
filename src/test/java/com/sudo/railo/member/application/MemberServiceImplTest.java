package com.sudo.railo.member.application;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

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
	private MemberDetail memberDetail;

	@BeforeEach
	void setUp() {
		memberDetail = MemberDetail.create(
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

	}

	// 서비스 계층에서 SecurityUtil 을 사용하고 있기 때문에 직접 SecurityContext 를 set
	@BeforeEach
	void setUpSecurityContext() {
		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken("202507020001", "testPwd", List.of(() -> "MEMBER"));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	@DisplayName("로그인 된 사용자의 이메일 변경 성공")
	@Test
	void updatedEmailSuccess() {

		//given
		UpdateEmailRequest request = new UpdateEmailRequest("updateMail@email.com");

		//when
		memberService.updatedEmail(request);

		//then
		assertThat(memberDetail.getEmail()).isEqualTo(request.newEmail());
	}

	@DisplayName("이메일 변경 실패 - 현재 사용하는 이메일과 동일")
	@Test
	void updatedEmailWithSameEmail() {

		// given
		String sameEmail = "test01@email.com";
		UpdateEmailRequest request = new UpdateEmailRequest(sameEmail);

		// when
		BusinessException exception = assertThrows(BusinessException.class, () -> memberService.updatedEmail(request));

		// then
		assertThat(exception.getErrorCode()).isEqualTo(MemberError.SAME_EMAIL);

	}

	@DisplayName("이메일 변경 실패 - 이미 사용중인 이메일")
	@Test
	void updatedEmailWithDuplicateEmail() {

		// given
		String duplicateEmail = "test02@email.com";
		UpdateEmailRequest request = new UpdateEmailRequest(duplicateEmail);

		// when
		BusinessException exception = assertThrows(BusinessException.class, () -> memberService.updatedEmail(request));

		// then
		assertThat(exception.getErrorCode()).isEqualTo(MemberError.DUPLICATE_EMAIL);

	}

	@DisplayName("로그인 된 사용자의 휴대폰 번호 변경 성공")
	@Test
	void updatedPhoneNumberSuccess() {

		//given
		UpdatePhoneNumberRequest request = new UpdatePhoneNumberRequest("01012341111");

		//when
		memberService.updatedPhoneNumber(request);

		//then
		assertThat(testMember.getPhoneNumber()).isEqualTo(request.newPhoneNumber());
	}

	@DisplayName("휴대폰 번호 변경 실패 - 현재 사용하는 휴대폰 번호와 동일")
	@Test
	void updatedEmailWithSamePhoneNumber() {

		// given
		String samePhoneNumber = "01012341234";
		UpdatePhoneNumberRequest request = new UpdatePhoneNumberRequest(samePhoneNumber);

		// when
		BusinessException exception = assertThrows(BusinessException.class,
			() -> memberService.updatedPhoneNumber(request));

		// then
		assertThat(exception.getErrorCode()).isEqualTo(MemberError.SAME_PHONE_NUMBER);

	}

	@DisplayName("휴대폰 번호 변경 실패 - 이미 사용중인 휴대폰 번호")
	@Test
	void updatedEmailWithDuplicatePhoneNumber() {

		// given
		String duplicatePhoneNumber = "01012345678";
		UpdatePhoneNumberRequest request = new UpdatePhoneNumberRequest(duplicatePhoneNumber);

		// when
		BusinessException exception = assertThrows(BusinessException.class,
			() -> memberService.updatedPhoneNumber(request));

		// then
		assertThat(exception.getErrorCode()).isEqualTo(MemberError.DUPLICATE_PHONE_NUMBER);
	}

	@DisplayName("로그인 된 사용자의 비밀번호 변경 성공")
	@Test
	void updatedPasswordSuccess() {

		//given
		UpdatePasswordRequest request = new UpdatePasswordRequest("updatePwd");

		//when
		memberService.updatedPassword(request);

		//then
		assertThat(passwordEncoder.matches(request.newPassword(), testMember.getPassword())).isTrue();
	}

	@DisplayName("비밀번호 변경 실패 - 현재 사용하는 비밀번호와 동일")
	@Test
	void updatedEmailWithSamePassword() {

		// given
		String samePassword = "testPwd";
		UpdatePasswordRequest request = new UpdatePasswordRequest(samePassword);

		// when
		BusinessException exception = assertThrows(BusinessException.class,
			() -> memberService.updatedPassword(request));

		// then
		assertThat(exception.getErrorCode()).isEqualTo(MemberError.SAME_PASSWORD);

	}

}
