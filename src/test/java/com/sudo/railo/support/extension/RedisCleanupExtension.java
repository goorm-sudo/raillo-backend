package com.sudo.railo.support.extension;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public class RedisCleanupExtension implements AfterEachCallback {

	@Override
	public void afterEach(ExtensionContext context) {
		RedisTemplate<String, String> redisTemplate = getRedisTemplate(context);
		assert redisTemplate.getConnectionFactory() != null;
		redisTemplate.getConnectionFactory()
			.getConnection()
			.serverCommands()
			.flushDb();
	}

	@SuppressWarnings("unchecked")
	private RedisTemplate<String, String> getRedisTemplate(ExtensionContext context) {
		return (RedisTemplate<String, String>)SpringExtension.getApplicationContext(context)
			.getBean("redisTemplate");
	}
}
