package com.sudo.railo.member.application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.redis.LogoutRedis;
import com.sudo.railo.global.redis.MemberRedis;
import com.sudo.railo.global.redis.RedisUtil;
import com.sudo.railo.global.security.TokenError;
import com.sudo.railo.global.security.jwt.TokenProvider;
import com.sudo.railo.global.security.util.SecurityUtil;
import com.sudo.railo.member.application.dto.request.MemberNoLoginRequest;
import com.sudo.railo.member.application.dto.request.SignUpRequest;
import com.sudo.railo.member.application.dto.request.TokenRequest;
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
	private final AuthenticationManagerBuilder authenticationManagerBuilder;
	private final TokenProvider tokenProvider;
	private final RedisUtil redisUtil;

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

		Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
		TokenResponse tokenResponse = tokenProvider.generateTokenDTO(authentication);

		// 레디스에 리프레시 토큰 저장
		MemberRedis memberRedis = new MemberRedis(request.memberNo(), tokenResponse.refreshToken());
		redisUtil.saveMemberToken(memberRedis);

		return tokenResponse;
	}

	@Override
	@Transactional
	public void logout(TokenRequest request) {
		// 로그아웃 하고 싶은 토큰이 유효한지 먼저 검증
		if (!tokenProvider.validateToken(request.accessToken())) {
			throw new BusinessException(TokenError.LOGOUT_ERROR);
		}

		System.out.println(SecurityUtil.getCurrentMemberNo());

		// AccessToken 에서 memberNo 를 가져옴
		Authentication authentication = tokenProvider.getAuthentication(request.accessToken());

		// Redis 에서 해당 memberNo 로 저장된 RefreshToken 이 있는지 여부 확인 후, 존재할 경우 삭제
		if (redisUtil.getRefreshToken(authentication.getName()) != null) {
			redisUtil.deleteRefreshToken(authentication.getName());
		}

		// 해당 AccessToken 유효 시간을 가져와 BlackList 에 저장
		Long expiration = tokenProvider.getAccessTokenExpiration(request.accessToken());
		LogoutRedis logoutRedis = new LogoutRedis("logout", expiration);
		redisUtil.saveLogoutToken(request.accessToken(), logoutRedis, expiration);
	}

}
