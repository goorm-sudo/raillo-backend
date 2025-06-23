package com.sudo.railo.global.security.jwt;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.redis.MemberRedis;
import com.sudo.railo.global.redis.RedisUtil;
import com.sudo.railo.global.security.TokenError;
import com.sudo.railo.member.application.dto.response.TokenResponse;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TokenProvider {

	private static final String AUTHORITIES_KEY = "auth";
	private static final String BEARER_TYPE = "Bearer";
	private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 30; // 30분
	private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 7;  // 7일
	private final Key key;
	private final RedisUtil redisUtil;

	public TokenProvider(
		@Value("${jwt.secret}") String secretKey, RedisUtil redisUtil
	) {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		this.key = Keys.hmacShaKeyFor(keyBytes);
		this.redisUtil = redisUtil;
	}

	// 토큰 생성 메서드
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

	public TokenResponse reissueAccessToken(String refreshToken) {

		// 리프레시 토큰에서 사용자 정보 추출 -> 클레임 확인
		Claims claims = parseClaims(refreshToken);

		// Refresh Token 검증 및 클레임에서 Refresh Token 여부 확인
		if (!validateToken(refreshToken) || claims.get("isRefreshToken") == null || !Boolean.TRUE.equals(
			claims.get("isRefreshToken"))) {
			throw new BusinessException(TokenError.INVALID_REFRESH_TOKEN);
		}

		String memberNo = claims.getSubject();
		String authorities = claims.get(AUTHORITIES_KEY).toString();

		String newAccessToken = generateAccessToken(memberNo, authorities);
		String newRefreshToken = generateRefreshToken(memberNo, authorities);

		MemberRedis memberRedis = new MemberRedis(memberNo, newRefreshToken);
		redisUtil.saveMemberToken(memberRedis);

		long now = System.currentTimeMillis();
		Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);

		return new TokenResponse(
			BEARER_TYPE,
			newAccessToken,
			accessTokenExpiresIn.getTime(),
			newRefreshToken
		);
	}

	// 토큰에 등록된 클레임의 sub에서 해당 회원 번호 추출
	public String getMemberNo(String accessToken) {
		return Jwts.parserBuilder()
			.setSigningKey(key)
			.build()
			.parseClaimsJws(accessToken)
			.getBody()
			.getSubject();
	}

	public Long getAccessTokenExpiration(String accessToken) {

		Claims claims = parseClaims(accessToken);

		Date expiration = claims.getExpiration();

		// 현재시간 기준으로 남은 유효시간 계산
		return expiration.getTime() - System.currentTimeMillis();
	}

	// AccessToken으로 인증 객체 추출
	public Authentication getAuthentication(String accessToken) {

		Claims claims = parseClaims(accessToken);

		if (claims.get(AUTHORITIES_KEY) == null) {
			throw new RuntimeException("권한 정보가 없는 토큰입니다.");
		}

		// 클레임에서 권한 정보 가져오기
		Collection<? extends GrantedAuthority> authorities =
			Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toList());

		UserDetails principal = new User(claims.getSubject(), "", authorities);

		return new UsernamePasswordAuthenticationToken(principal, "", authorities);
	}

	// 토큰 유효성 검사
	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token);
			return true;
		} catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
			log.info("잘못된 JWT 서명입니다.");
		} catch (ExpiredJwtException e) {
			log.info("만료된 JWT 토큰입니다.");
		} catch (UnsupportedJwtException e) {
			log.info("지원되지 않는 JWT 토큰입니다.");
		} catch (IllegalArgumentException e) {
			log.info("JWT 토큰이 잘못되었습니다.");
		}
		return false;
	}

	// AccessToken에서 클레임을 추출하는 메서드
	private Claims parseClaims(String accessToken) {
		try {
			return Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(accessToken)
				.getBody();
		} catch (ExpiredJwtException e) {
			// 토큰이 만료되어 예외가 발생하더라도 클레임 값들은 뽑을 수 있음
			return e.getClaims();
		}
	}

}
