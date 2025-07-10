package com.sudo.railo.member.application;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.application.dto.request.UpdatePasswordRequest;
import com.sudo.railo.member.application.dto.request.UpdatePhoneNumberRequest;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.domain.MemberDetail;
import com.sudo.railo.member.domain.Membership;
import com.sudo.railo.member.domain.Role;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infra.MemberRepository;

@ExtendWith(MockitoExtension.class)
class MemberServiceUnitTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private BCryptPasswordEncoder passwordEncoder;

	@InjectMocks
	private MemberServiceImpl memberService;

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

		// 서비스 계층에서 SecurityUtil 을 사용하고 있기 때문에 직접 SecurityContext 를 set
		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken("202507020001", "testPwd", List.of(() -> "MEMBER"));
		SecurityContextHolder.getContext().setAuthentication(authentication);

	}

	@AfterEach
	void tearDown() {
		Mockito.reset(memberRepository);
		SecurityContextHolder.clearContext();
	}

	@DisplayName("이메일 변경 실패 - 현재 사용하는 이메일과 동일")
	@Test
	void updateEmailWithSameEmail() {

		// given
		String sameEmail = "test01@email.com";

		Mockito.when(memberRepository.findByMemberNo(Mockito.anyString())).thenReturn(Optional.of(testMember));

		// when & then
		assertThatThrownBy(() -> memberService.updateEmail(sameEmail))
			.isInstanceOf(BusinessException.class)
			.hasMessage(MemberError.SAME_EMAIL.getMessage());

	}

	@DisplayName("이메일 변경 실패 - 이미 사용중인 이메일")
	@Test
	void updateEmailWithDuplicateEmail() {

		// given
		String duplicateEmail = "test02@email.com";

		Mockito.when(memberRepository.findByMemberNo(Mockito.anyString())).thenReturn(Optional.of(testMember));
		Mockito.when(memberRepository.existsByMemberDetailEmail(duplicateEmail)).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> memberService.updateEmail(duplicateEmail))
			.isInstanceOf(BusinessException.class)
			.hasMessage(MemberError.DUPLICATE_EMAIL.getMessage());
	}

	@DisplayName("휴대폰 번호 변경 실패 - 현재 사용하는 휴대폰 번호와 동일")
	@Test
	void updatePhoneNumberWithSamePhoneNumber() {

		// given
		String samePhoneNumber = "01012341234";
		UpdatePhoneNumberRequest request = new UpdatePhoneNumberRequest(samePhoneNumber);

		Mockito.when(memberRepository.findByMemberNo(Mockito.anyString())).thenReturn(Optional.of(testMember));

		// when & then
		assertThatThrownBy(() -> memberService.updatePhoneNumber(request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(MemberError.SAME_PHONE_NUMBER.getMessage());

	}

	@DisplayName("휴대폰 번호 변경 실패 - 이미 사용중인 휴대폰 번호")
	@Test
	void updatePhoneNumberWithDuplicatePhoneNumber() {

		// given
		String duplicatePhoneNumber = "01012345678";
		UpdatePhoneNumberRequest request = new UpdatePhoneNumberRequest(duplicatePhoneNumber);

		Mockito.when(memberRepository.findByMemberNo(Mockito.anyString())).thenReturn(Optional.of(testMember));
		Mockito.when(memberRepository.existsByPhoneNumber(duplicatePhoneNumber)).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> memberService.updatePhoneNumber(request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(MemberError.DUPLICATE_PHONE_NUMBER.getMessage());
	}

	@DisplayName("비밀번호 변경 실패 - 현재 사용하는 비밀번호와 동일")
	@Test
	void updatePasswordWithSamePassword() {

		// given
		String samePassword = "testPwd";
		UpdatePasswordRequest request = new UpdatePasswordRequest(samePassword);

		Mockito.when(memberRepository.findByMemberNo(Mockito.anyString())).thenReturn(Optional.of(testMember));
		Mockito.when(passwordEncoder.matches(samePassword, testMember.getPassword())).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> memberService.updatePassword(request))
			.isInstanceOf(BusinessException.class)
			.hasMessage(MemberError.SAME_PASSWORD.getMessage());

	}

}
