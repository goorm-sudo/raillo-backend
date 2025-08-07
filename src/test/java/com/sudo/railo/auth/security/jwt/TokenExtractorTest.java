package com.sudo.railo.auth.security.jwt;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;

import com.sudo.railo.auth.exception.TokenError;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.support.annotation.ServiceTest;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@ServiceTest
class TokenExtractorTest {

	private static final String TEST_SECRET_KEY = "crailotestjwtsecretkey2025fordevelopmentandtestingonlylonglonglonglonglonglonglonglonglong==";
	private static final String MEMBER_NO = "test";
	private static final String AUTHORITIES = "ROLE_MEMBER";

	private TokenExtractor tokenExtractor;
	private SecretKey testKey;

	@BeforeEach
	void setUp() {
		byte[] keyBytes = Decoders.BASE64.decode(TEST_SECRET_KEY);
		testKey = Keys.hmacShaKeyFor(keyBytes);
		tokenExtractor = new TokenExtractor(TEST_SECRET_KEY);
	}

	@Test
	@DisplayName("Authorization 헤더에서 Bearer 토큰을 추출한다.")
	void resolveToken() {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();
		String token = "test-token";
		request.addHeader("Authorization", "Bearer " + token);

		// when
		String result = tokenExtractor.resolveToken(request);

		// then
		assertThat(result).isEqualTo(token);
	}

	@Test
	@DisplayName("Authorization 헤더가 없으면 null을 반환한다.")
	void resolveTokenWithoutHeader() {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();

		// when
		String result = tokenExtractor.resolveToken(request);

		// then
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Authorization 헤더가 Bearer로 시작하지 않으면 null을 반환한다.")
	void resolveTokenWithoutBearerPrefix() {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Basic test-token");

		// when
		String result = tokenExtractor.resolveToken(request);

		// then
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("유효한 토큰에서 회원번호를 추출한다.")
	void getMemberNo() {
		// given
		String token = createValidToken();

		// when
		String result = tokenExtractor.getMemberNo(token);

		// then
		assertThat(result).isEqualTo(MEMBER_NO);
	}

	@Test
	@DisplayName("유효한 AccessToken 에서 남은 유효시간을 추출한다.")
	void getAccessTokenExpiration() {
		// given
		long expirationTime = System.currentTimeMillis() + (30 * 60 * 1000);
		String token = createTokenWithExpiration(expirationTime);

		// when
		Duration result = tokenExtractor.getAccessTokenExpiration(token);

		// then
		assertThat(result.toMillis()).isBetween(29 * 60 * 1000L, 30 * 60 * 1000L);
	}

	@Test
	@DisplayName("유효한 토큰에서 인증 정보를 추출한다.")
	void getAuthentication() {
		// given
		String token = createValidToken();

		// when
		Authentication result = tokenExtractor.getAuthentication(token);

		// then
		assertThat(result.getName()).isEqualTo(MEMBER_NO);
		assertThat(result.getAuthorities()).hasSize(1);
		assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo(AUTHORITIES);
	}

	@Test
	@DisplayName("권한 정보가 없는 토큰에서 인증 정보를 추출하려고 하면 예외가 발생한다.")
	void getAuthenticationWithoutAuthorities() {
		// given
		String tokenWithoutAuth = Jwts.builder()
			.setSubject(MEMBER_NO)
			.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60))
			.signWith(testKey, SignatureAlgorithm.HS512)
			.compact();

		// when & then
		assertThatThrownBy(() -> tokenExtractor.getAuthentication(tokenWithoutAuth))
			.isInstanceOf(BusinessException.class)
			.hasMessage(TokenError.AUTHORITY_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("유효한 토큰에서 클레임을 추출한다.")
	void parseClaims() {
		// given
		String token = createValidToken();

		// when
		Claims result = tokenExtractor.parseClaims(token);

		// then
		assertThat(result.getSubject()).isEqualTo(MEMBER_NO);
		assertThat(result.get(TokenExtractor.AUTHORITIES_KEY)).isEqualTo(AUTHORITIES);
	}

	@Test
	@DisplayName("만료된 토큰에서도 클레임을 추출한다.")
	void parseClaimsFromExpiredToken() {
		// given
		String expiredToken = Jwts.builder()
			.setSubject(MEMBER_NO)
			.claim(TokenExtractor.AUTHORITIES_KEY, AUTHORITIES)
			.setExpiration(new Date(System.currentTimeMillis() - 1000))
			.signWith(testKey, SignatureAlgorithm.HS512)
			.compact();

		// when
		Claims result = tokenExtractor.parseClaims(expiredToken);

		// then
		assertThat(result.getSubject()).isEqualTo(MEMBER_NO);
		assertThat(result.get(TokenExtractor.AUTHORITIES_KEY)).isEqualTo(AUTHORITIES);
	}

	private String createValidToken() {
		return Jwts.builder()
			.setSubject(MEMBER_NO)
			.claim(TokenExtractor.AUTHORITIES_KEY, AUTHORITIES)
			.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60))
			.signWith(testKey, SignatureAlgorithm.HS512)
			.compact();
	}

	private String createTokenWithExpiration(long expirationTime) {
		return Jwts.builder()
			.setSubject(MEMBER_NO)
			.claim(TokenExtractor.AUTHORITIES_KEY, AUTHORITIES)
			.setExpiration(new Date(expirationTime))
			.signWith(testKey, SignatureAlgorithm.HS512)
			.compact();
	}
}
