package com.sudo.railo.auth.security.jwt;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import com.sudo.railo.auth.exception.TokenError;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.support.annotation.ServiceTest;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@ServiceTest
class TokenGeneratorTest {

	private static final String TEST_SECRET_KEY = "crailotestjwtsecretkey2025fordevelopmentandtestingonlylonglonglonglonglonglonglonglonglong==";

	@Autowired
	private TokenGenerator tokenGenerator;

	@Mock
	private Authentication authentication;

	private TokenValidator tokenValidator;

	private SecretKey testKey;

	@BeforeEach
	void setUp() {
		byte[] keyBytes = Decoders.BASE64.decode(TEST_SECRET_KEY);
		testKey = Keys.hmacShaKeyFor(keyBytes);
		tokenValidator = new TokenValidator(TEST_SECRET_KEY);
		TokenExtractor tokenExtractor = new TokenExtractor(TEST_SECRET_KEY);
		tokenGenerator = new TokenGenerator(TEST_SECRET_KEY, tokenValidator, tokenExtractor);
	}

	@Test
	@DisplayName("Authentication에 있는 회원 정보로 토큰을 발급한다.")
	void generateTokens() {
		String memberNo = "test";
		when(authentication.getName()).thenReturn(memberNo);

		long beforeTime = System.currentTimeMillis();
		var result = tokenGenerator.generateTokenDTO(authentication);
		long afterTime = System.currentTimeMillis();

		assertThat(result.grantType()).isEqualTo("Bearer");
		tokenValidator.validateToken(result.accessToken());
		tokenValidator.validateToken(result.refreshToken());
		assertThat(result.accessTokenExpiresIn()).isBetween(beforeTime + (30 * 60 * 1000),
			afterTime + (30 * 60 * 1000));
	}

	@Test
	@DisplayName("유효한 refreshToken 으로 accessToken을 재발급한다.")
	void refreshAccessToken() {
		String memberNo = "test";
		when(authentication.getName()).thenReturn(memberNo);
		var tokenResponse = tokenGenerator.generateTokenDTO(authentication);

		long beforeTime = System.currentTimeMillis();
		var result = tokenGenerator.reissueAccessToken(tokenResponse.refreshToken());
		long afterTime = System.currentTimeMillis();

		tokenValidator.validateToken(result.accessToken());
		assertThat(result.accessTokenExpiresIn()).isBetween(beforeTime + (30 * 60 * 1000),
			afterTime + (30 * 60 * 1000));
	}

	@Test
	@DisplayName("회원 정보로 TemporaryToken을 발급한다.")
	void generateTemporaryToken() {
		String memberNo = "test";

		String temporaryToken = tokenGenerator.generateTemporaryToken(memberNo);

		tokenValidator.validateToken(temporaryToken);
	}

	@Test
	@DisplayName("만료된 refreshToken 으로 accessToken 재발급을 시도하면 예외가 발생한다.")
	void shouldThrowExceptionWhenReissuingAccessTokenWithInvalidRefreshToken() {
		String memberNo = "test";
		String authorities = "ROLE_MEMBER";
		String invalidRefreshToken = Jwts.builder()
			.setSubject(memberNo)
			.claim("auth", authorities)
			.claim("isRefreshToken", true)
			.setExpiration(new Date(System.currentTimeMillis()- 1000))
			.signWith(testKey, SignatureAlgorithm.HS512)
			.compact();

		assertThatThrownBy(() -> tokenGenerator.reissueAccessToken(invalidRefreshToken))
			.isInstanceOf(BusinessException.class)
			.hasMessage(TokenError.INVALID_REFRESH_TOKEN.getMessage());
	}

	@Test
	@DisplayName("isRefreshToken 클레임이 없는 refreshToken 으로 accessToken 재발급을 시도하면 예외가 발생한다.")
	void shouldThrowExceptionWhenReissuingAccessTokenWithoutRefreshTokenClaim() {
		String memberNo = "test";
		String authorities = "ROLE_MEMBER";
		String tokenWithoutClaim = Jwts.builder()
			.setSubject(memberNo)
			.claim("auth", authorities)
			.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60))
			.signWith(testKey, SignatureAlgorithm.HS512)
			.compact();

		assertThatThrownBy(() -> tokenGenerator.reissueAccessToken(tokenWithoutClaim))
			.isInstanceOf(BusinessException.class)
			.hasMessage(TokenError.INVALID_REFRESH_TOKEN.getMessage());
	}

	@Test
	@DisplayName("isRefreshToken 클레임이 false인 refreshToken 으로 accessToken 재발급을 시도하면 예외가 발생한다.")
	void shouldThrowExceptionWhenReissuingAccessTokenWithFalseRefreshTokenClaim() {
		String memberNo = "test";
		String authorities = "ROLE_MEMBER";
		String tokenWithFalseClaim = Jwts.builder()
			.setSubject(memberNo)
			.claim("auth", authorities)
			.claim("isRefreshToken", false)
			.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60))
			.signWith(testKey, SignatureAlgorithm.HS512)
			.compact();

		assertThatThrownBy(() -> tokenGenerator.reissueAccessToken(tokenWithFalseClaim))
			.isInstanceOf(BusinessException.class)
			.hasMessage(TokenError.INVALID_REFRESH_TOKEN.getMessage());
	}
}
