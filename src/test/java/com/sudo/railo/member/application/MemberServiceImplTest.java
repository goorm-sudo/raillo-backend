package com.sudo.railo.member.application;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.application.dto.request.UpdateEmailRequest;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.domain.MemberDetail;
import com.sudo.railo.member.domain.Membership;
import com.sudo.railo.member.domain.Role;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infra.MemberRepository;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

	@Mock
	private MemberRepository memberRepository;

	@InjectMocks
	private MemberServiceImpl memberService;

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
			"testPwd",
			Role.MEMBER,
			memberDetail
		);

		// Mock 객체는 상태를 저장하지 않으므로 검증만 진행
		when(memberRepository.findByMemberNo("202507020001")).thenReturn(Optional.of(testMember));

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
		verify(memberRepository, times(1)).save(testMember);
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

		when(memberRepository.existsByMemberDetailEmail(duplicateEmail)).thenReturn(true);

		// when
		BusinessException exception = assertThrows(BusinessException.class, () -> memberService.updatedEmail(request));

		// then
		assertThat(exception.getErrorCode()).isEqualTo(MemberError.DUPLICATE_EMAIL);

	}
}
