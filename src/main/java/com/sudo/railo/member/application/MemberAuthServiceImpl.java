package com.sudo.railo.member.application;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.redis.AuthRedisRepository;
import com.sudo.railo.global.redis.LogoutToken;
import com.sudo.railo.global.security.TokenError;
import com.sudo.railo.global.security.jwt.TokenProvider;
import com.sudo.railo.member.application.dto.request.MemberNoLoginRequest;
import com.sudo.railo.member.application.dto.request.SignUpRequest;
import com.sudo.railo.member.application.dto.response.ReissueTokenResponse;
import com.sudo.railo.member.application.dto.response.SendCodeResponse;
import com.sudo.railo.member.application.dto.response.SignUpResponse;
import com.sudo.railo.member.application.dto.response.TokenResponse;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.domain.MemberDetail;
import com.sudo.railo.member.domain.Membership;
import com.sudo.railo.member.domain.Role;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infra.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberAuthServiceImpl implements MemberAuthService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final MemberNoGenerator memberNoGenerator;
	private final EmailAuthService emailAuthService;
	private final AuthenticationManager authenticationManager;
	private final TokenProvider tokenProvider;
	private final AuthRedisRepository authRedisRepository;

	@Override
	@Transactional
	public SignUpResponse signUp(SignUpRequest request) {

		if (memberRepository.existsByMemberDetailEmail(request.email())) {
			throw new BusinessException(MemberError.DUPLICATE_EMAIL);
		}

		String memberNo = memberNoGenerator.generateMemberNo();
		LocalDate birthDate = LocalDate.parse(request.birthDate(), DateTimeFormatter.ISO_LOCAL_DATE);

		MemberDetail memberDetail = MemberDetail.create(memberNo, Membership.BUSINESS, request.email(), birthDate,
			request.gender());
		Member member = Member.create(request.name(), request.phoneNumber(), passwordEncoder.encode(request.password()),
			Role.MEMBER, memberDetail);

		memberRepository.save(member);

		return new SignUpResponse(memberNo);
	}

	@Override
	@Transactional
	public TokenResponse memberNoLogin(MemberNoLoginRequest request) {

		UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
			request.memberNo(), request.password());

		Authentication authentication = authenticationManager.authenticate(authenticationToken);
		TokenResponse tokenResponse = tokenProvider.generateTokenDTO(authentication);

		// 레디스에 리프레시 토큰 저장
		authRedisRepository.saveRefreshToken(request.memberNo(), tokenResponse.refreshToken());

		return tokenResponse;
	}

	@Override
	@Transactional
	public void logout(String accessToken, String memberNo) {

		// Redis 에서 해당 memberNo 로 저장된 RefreshToken 이 있는지 여부 확인 후, 존재할 경우 삭제
		if (authRedisRepository.getRefreshToken(memberNo) != null) {
			authRedisRepository.deleteRefreshToken(memberNo);
		}

		// 해당 AccessToken 유효 시간을 가져와 BlackList 에 저장
		Duration expiration = tokenProvider.getAccessTokenExpiration(accessToken);
		LogoutToken logoutToken = new LogoutToken("logout", expiration);
		authRedisRepository.saveLogoutToken(accessToken, logoutToken, expiration);
	}

	@Override
	@Transactional
	public ReissueTokenResponse reissueAccessToken(String refreshToken, String memberNo) {

		String restoredRefreshToken = authRedisRepository.getRefreshToken(memberNo);

		if (!refreshToken.equals(restoredRefreshToken)) {
			throw new BusinessException(TokenError.NOT_EQUALS_REFRESH_TOKEN);
		}

		return tokenProvider.reissueAccessToken(refreshToken);
	}

	/* 이메일 인증 관련 */
	@Override
	public SendCodeResponse sendAuthCode(String email) {
		String code = createAuthCode();
		emailAuthService.sendEmail(email, code);
		// redis 에 유효시간 설정해서 인증코드 저장
		authRedisRepository.saveAuthCode(email, code);

		return new SendCodeResponse(email);
	}

	@Override
	public boolean verifyAuthCode(String email, String authCode) {
		// redis 에서 저장해둔 인증 코드 get
		String findCode = authRedisRepository.getAuthCode(email);
		boolean isVerified = authCode.equals(findCode);

		if (isVerified) {
			authRedisRepository.deleteAuthCode(email);
		}

		return isVerified;
	}

	private String createAuthCode() {
		SecureRandom random = new SecureRandom();
		return String.format("%06d", random.nextInt(1000000));
	}
}
