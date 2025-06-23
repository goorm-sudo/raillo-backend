package com.sudo.railo.global.redis;

import org.springframework.data.redis.core.RedisHash;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@RedisHash(value = "MemberToken", timeToLive = 3600 * 24 * 7)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class MemberRedis {

	@Id
	private String memberNo;
	private String refreshToken;
}
