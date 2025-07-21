package com.sudo.railo.support.extension;

import java.io.IOException;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import redis.embedded.RedisServer;

public class RedisServerExtension implements BeforeAllCallback, AfterAllCallback {
	private static RedisServer redisServer;

	@Override
	public void beforeAll(ExtensionContext context) throws IOException {
		if (redisServer == null) {
			redisServer = new RedisServer(63790);
			redisServer.start();
		}
	}

	@Override
	public void afterAll(ExtensionContext context) throws IOException {
		if (redisServer != null) {
			redisServer.stop();
			redisServer = null;
		}
	}
}
