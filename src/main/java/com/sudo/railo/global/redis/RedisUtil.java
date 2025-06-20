package com.sudo.railo.global.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisUtil {

	private final RedisTemplate<String, Object> redisTemplate;

	public void save(String key, String value) {
		redisTemplate.opsForValue().set(key, value);
	}
}
