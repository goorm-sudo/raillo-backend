package com.sudo.railo.auth.application;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.sudo.railo.auth.application.dto.request.LoginRequest;
import com.sudo.railo.auth.application.dto.request.SignUpRequest;
import com.sudo.railo.auth.application.dto.response.ReissueTokenResponse;
import com.sudo.railo.auth.application.dto.response.SignUpResponse;
import com.sudo.railo.auth.application.dto.response.TokenResponse;
import com.sudo.railo.auth.exception.TokenError;
import com.sudo.railo.auth.security.jwt.TokenGenerator;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.redis.AuthRedisRepository;
import com.sudo.railo.global.redis.RedisKeyGenerator;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.domain.MemberDetail;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infrastructure.MemberRepository;
import com.sudo.railo.support.annotation.ServiceTest;
import com.sudo.railo.support.fixture.MemberFixture;

@ServiceTest
class AuthServiceTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private AuthRedisRepository authRedisRepository;

	@Autowired
	private RedisTemplate<String, Object> objectRedisTemplate;

	@Autowired
	private RedisKeyGenerator redisKeyGenerator;

	@Autowired
	private TokenGenerator tokenGenerator;

	@Test
	@DisplayName("회원가입에 성공한다.")
	void signUp_success() {
		//given
		SignUpRequest request = new SignUpRequest("김이름", "01012341234", "testPwd", "test@example.com", "1990-01-01",
			"M");

		//when
		SignUpResponse response = authService.signUp(request);

		//then
		Member savedMember = memberRepository.findByMemberNo(response.memberNo())
			.orElseThrow(() -> new AssertionError("회원 정보가 DB에 저장되지 않았습니다."));
		MemberDetail savedMemberDetail = savedMember.getMemberDetail();

		assertThat(savedMember.getName()).isEqualTo(request.name());
		assertThat(savedMember.getPhoneNumber()).isEqualTo(request.phoneNumber());
		assertThat(passwordEncoder.matches(request.password(), savedMember.getPassword())).isTrue();
		assertThat(response.memberNo()).isNotNull();
		assertThat(savedMemberDetail.getEmail()).isEqualTo(request.email());
		assertThat(savedMemberDetail.getBirthDate()).isEqualTo(request.birthDate());
		assertThat(savedMemberDetail.getGender()).isEqualTo(request.gender());
	}

	@Test
	@DisplayName("중복된 이메일로 회원가입 시도 시 실패한다.")
	void signUp_fail() {
		//given
		Member member = MemberFixture.createStandardMember();
		memberRepository.save(member);

		SignUpRequest request = new SignUpRequest("김이름", "01012341234", "testPwd", "test@example.com", "1990-01-01",
			"M");

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> authService.signUp(request))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberError.DUPLICATE_EMAIL));
	}

	@Test
	@DisplayName("회원이 로그인에 성공한다.")
	void login_success() {
		//given
		Member member = createMemberWithEncryptedPassword();
		String memberNo = member.getMemberDetail().getMemberNo();

		LoginRequest request = new LoginRequest(memberNo, "testPassword");

		//when
		TokenResponse response = authService.login(request);

		//then
		assertThat(response.grantType()).isEqualTo("Bearer");
		assertThat(response.accessToken()).isNotEmpty();
		assertThat(response.refreshToken()).isNotEmpty();
		assertThat(response.accessTokenExpiresIn()).isNotNull();

		String savedRefreshToken = authRedisRepository.getRefreshToken(member.getMemberDetail().getMemberNo());
		assertThat(savedRefreshToken).isEqualTo(response.refreshToken());
	}

	@Test
	@DisplayName("회원이 로그아웃에 성공한다.")
	void logout_success() {
		//given
		Member member = createMemberWithEncryptedPassword();
		String memberNo = member.getMemberDetail().getMemberNo();

		LoginRequest request = new LoginRequest(memberNo, "testPassword");
		TokenResponse response = authService.login(request);

		String accessToken = response.accessToken();

		String logoutTokenKey = redisKeyGenerator.generateLogoutTokenKey(accessToken);

		//when
		authService.logout(accessToken, memberNo);

		//then
		assertThat(authRedisRepository.getRefreshToken(memberNo)).isNull();
		assertThat(objectRedisTemplate.hasKey(logoutTokenKey)).isTrue();
	}

	@Test
	@DisplayName("리프레시 토큰만 유효시간이 만료되어 레디스에 존재하지 않아도 로그아웃에 성공한다.")
	void logout_success_when_refresh_token_is_expired() {
		//given
		Member member = createMemberWithEncryptedPassword();
		String memberNo = member.getMemberDetail().getMemberNo();

		LoginRequest request = new LoginRequest(memberNo, "testPassword");
		TokenResponse response = authService.login(request);

		String accessToken = response.accessToken();
		String logoutTokenKey = redisKeyGenerator.generateLogoutTokenKey(accessToken);

		authRedisRepository.deleteRefreshToken(memberNo); // 리프레시 토큰을 삭제하여 만료된 상황 시뮬레이션

		//when
		authService.logout(accessToken, memberNo);

		//then
		assertThat(authRedisRepository.getRefreshToken(memberNo)).isNull();
		assertThat(objectRedisTemplate.hasKey(logoutTokenKey)).isTrue();
	}

	@Test
	@DisplayName("액세스 토큰 재발급에 성공한다.")
	void reissueAccessToken_success() {
		//given
		Member member = createMemberWithEncryptedPassword();
		String memberNo = member.getMemberDetail().getMemberNo();

		LoginRequest request = new LoginRequest(memberNo, "testPassword");
		TokenResponse tokenResponse = authService.login(request);
		String refreshToken = tokenResponse.refreshToken();

		//when
		ReissueTokenResponse reissueTokenResponse = authService.reissueAccessToken(refreshToken);

		//then
		assertThat(reissueTokenResponse.grantType()).isEqualTo("Bearer");
		assertThat(reissueTokenResponse.accessToken()).isNotEmpty();
		assertThat(reissueTokenResponse.accessTokenExpiresIn()).isNotNull();
	}

	@Test
	@DisplayName("저장된 리프레시 토큰과 일치하지 않을 경우 액세스 토큰 재발급에 실패한다.")
	void reissueAccessToken_fail() {
		//given
		Member member = MemberFixture.createStandardMember();
		memberRepository.save(member);

		// refreshToken 이 일치하지 않도록 테스트 인증 객체 따로 생성 후 토큰 생성
		List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(member.getRole().toString()));
		UserDetails userDetails = new User(member.getMemberDetail().getMemberNo(), member.getPassword(), authorities);
		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, member.getPassword(),
			authorities);

		TokenResponse tokenResponse = tokenGenerator.generateTokenDTO(authentication);
		String refreshToken = tokenResponse.refreshToken();

		String diffRefreshToken = refreshToken + "diff";
		authRedisRepository.saveRefreshToken(member.getMemberDetail().getMemberNo(), diffRefreshToken);

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> authService.reissueAccessToken(refreshToken))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(TokenError.NOT_EQUALS_REFRESH_TOKEN));
	}

	private Member createMemberWithEncryptedPassword() {
		Member member = MemberFixture.createStandardMember();
		String plainPwd = member.getPassword();
		String encodedPwd = passwordEncoder.encode(plainPwd);

		member.updatePassword(encodedPwd);

		return memberRepository.save(member);
	}

}
