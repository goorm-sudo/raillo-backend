package com.sudo.railo.global.redis;

import org.springframework.data.redis.core.RedisHash;

import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@RedisHash(value = "MemberToken", timeToLive = 3600 * 24 * 14)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberRedis {

	@Id
	private String memberNo;
	private String refreshToken;
}
