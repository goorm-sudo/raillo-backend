package com.sudo.railo.auth.security.jwt;

import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.sudo.railo.auth.application.dto.response.ReissueTokenResponse;
import com.sudo.railo.auth.application.dto.response.TokenResponse;
import com.sudo.railo.auth.exception.TokenError;
import com.sudo.railo.global.exception.error.BusinessException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class TokenGenerator {

	private final TokenValidator tokenValidator;
	private final TokenExtractor tokenExtractor;
	private final Key key;

	private static final String AUTHORITIES_KEY = "auth";
	private static final String BEARER_TYPE = "Bearer";
	private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 30; // 30분
	private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 7;  // 7일
	private static final long TEMPORARY_TOKEN_EXPIRE_TIME = 1000 * 60 * 5; // 5분

	public TokenGenerator(@Value("${jwt.secret}") String secretKey, TokenValidator tokenValidator,
		TokenExtractor tokenExtractor) {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		this.key = Keys.hmacShaKeyFor(keyBytes);
		this.tokenValidator = tokenValidator;
		this.tokenExtractor = tokenExtractor;
	}

	/**
	 * 토큰 생성 메서드
	 * */
	public TokenResponse generateTokenDTO(Authentication authentication) {

		// 권한들 가져오기
		String authorities = authentication.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.joining(","));

		String accessToken = generateAccessToken(authentication.getName(), authorities);
		String refreshToken = generateRefreshToken(authentication.getName(), authorities);

		long now = System.currentTimeMillis();
		Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);

		return new TokenResponse(
			BEARER_TYPE,
			accessToken,
			accessTokenExpiresIn.getTime(),
			refreshToken
		);
	}

	/**
	 * AccessToken 재발급
	 * */
	public ReissueTokenResponse reissueAccessToken(String refreshToken) {

		// 리프레시 토큰에서 사용자 정보 추출 -> 클레임 확인
		Claims claims = tokenExtractor.parseClaims(refreshToken);

		// Refresh Token 검증 및 클레임에서 Refresh Token 여부 확인
		if (!tokenValidator.validateToken(refreshToken) || claims.get("isRefreshToken") == null || !Boolean.TRUE.equals(
			claims.get("isRefreshToken"))) {
			throw new BusinessException(TokenError.INVALID_REFRESH_TOKEN);
		}

		String memberNo = claims.getSubject();
		String authorities = claims.get(AUTHORITIES_KEY).toString();

		String newAccessToken = generateAccessToken(memberNo, authorities);

		long now = System.currentTimeMillis();
		Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);

		return new ReissueTokenResponse(
			BEARER_TYPE,
			newAccessToken,
			accessTokenExpiresIn.getTime()
		);
	}

	/**
	 * 임시토큰 - TemporaryToken 생성
	 * */
	public String generateTemporaryToken(String memberNo) {
		long now = System.currentTimeMillis();
		Date temporaryTokenExpiresIn = new Date(now + TEMPORARY_TOKEN_EXPIRE_TIME);
		return Jwts.builder()
			.setHeaderParam(Header.TYPE, Header.JWT_TYPE)
			.setSubject(memberNo)
			.claim(AUTHORITIES_KEY, "TEMPORARY_TOKEN")
			.setExpiration(temporaryTokenExpiresIn)
			.signWith(key, SignatureAlgorithm.HS512)
			.compact();
	}

	/**
	 * AccessToken 생성
	 * */
	private String generateAccessToken(String memberNo, String authorities) {
		long now = System.currentTimeMillis();
		Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);
		return Jwts.builder()
			.setHeaderParam(Header.TYPE, Header.JWT_TYPE)
			.setSubject(memberNo)
			.claim(AUTHORITIES_KEY, authorities)
			.setExpiration(accessTokenExpiresIn)
			.signWith(key, SignatureAlgorithm.HS512)
			.compact();
	}

	/**
	 * RefreshToken 생성
	 * */
	private String generateRefreshToken(String memberNo, String authorities) {
		long now = System.currentTimeMillis();
		Date refreshTokenExpiresIn = new Date(now + REFRESH_TOKEN_EXPIRE_TIME);
		return Jwts.builder()
			.setHeaderParam(Header.TYPE, Header.JWT_TYPE)
			.setSubject(memberNo)
			.claim(AUTHORITIES_KEY, authorities)
			.setExpiration(refreshTokenExpiresIn)
			.claim("isRefreshToken", true) // refreshToken 임을 나타내는 클레임 추가
			.signWith(key, SignatureAlgorithm.HS512)
			.compact();
	}

}
