package com.sudo.railo.auth.security.jwt;

import static org.assertj.core.api.Assertions.*;

import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sudo.railo.support.annotation.ServiceTest;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@ServiceTest
class TokenValidatorTest {

	private static final String TEST_SECRET_KEY = "crailotestjwtsecretkey2025fordevelopmentandtestingonlylonglonglonglonglonglonglonglonglong==";
	private static final String MEMBER_NO = "test";

	private TokenValidator tokenValidator;
	private SecretKey testKey;

	@BeforeEach
	void setUp() {
		byte[] keyBytes = Decoders.BASE64.decode(TEST_SECRET_KEY);
		testKey = Keys.hmacShaKeyFor(keyBytes);
		tokenValidator = new TokenValidator(TEST_SECRET_KEY);
	}

	@Test
	@DisplayName("유효한 토큰은 검증을 통과한다.")
	void validateValidToken() {
		// given
		String validToken = createValidToken();

		// when
		boolean result = tokenValidator.validateToken(validToken);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("잘못된 서명의 토큰은 검증에 실패한다.")
	void validateTokenWithInvalidSignature() {
		// given
		String invalidKey = "invalidsecretkeyinvalidsecretkey2025fordevelopmentandtestingonlylonglonglonglonglonglonglonglonglong==";
		byte[] invalidKeyBytes = Decoders.BASE64.decode(invalidKey);
		SecretKey invalidSecretKey = Keys.hmacShaKeyFor(invalidKeyBytes);

		String tokenWithInvalidSignature = Jwts.builder()
			.setSubject(MEMBER_NO)
			.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60))
			.signWith(invalidSecretKey, SignatureAlgorithm.HS512)
			.compact();

		// when
		boolean result = tokenValidator.validateToken(tokenWithInvalidSignature);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("만료된 토큰은 검증에 실패한다.")
	void validateExpiredToken() {
		// given
		String expiredToken = Jwts.builder()
			.setSubject(MEMBER_NO)
			.setExpiration(new Date(System.currentTimeMillis() - 1000))
			.signWith(testKey, SignatureAlgorithm.HS512)
			.compact();

		// when
		boolean result = tokenValidator.validateToken(expiredToken);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("형식이 잘못된 토큰은 검증에 실패한다.")
	void validateMalformedToken() {
		// given
		String malformedToken = "malformed.token.string";

		// when
		boolean result = tokenValidator.validateToken(malformedToken);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("null 또는 빈 토큰은 검증에 실패한다.")
	void validateNullOrEmptyToken() {
		// when
		boolean resultNull = tokenValidator.validateToken(null);
		boolean resultEmpty = tokenValidator.validateToken("");

		// then
		assertThat(resultNull).isFalse();
		assertThat(resultEmpty).isFalse();
	}

	@Test
	@DisplayName("지원되지 않는 JWT 토큰은 검증에 실패한다.")
	void validateUnsupportedToken() {
		// given
		String unsignedToken = Jwts.builder()
			.setSubject(MEMBER_NO)
			.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60))
			.compact();

		// when
		boolean result = tokenValidator.validateToken(unsignedToken);

		// then
		assertThat(result).isFalse();
	}

	private String createValidToken() {
		return Jwts.builder()
			.setSubject(MEMBER_NO)
			.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60))
			.signWith(testKey, SignatureAlgorithm.HS512)
			.compact();
	}
}
