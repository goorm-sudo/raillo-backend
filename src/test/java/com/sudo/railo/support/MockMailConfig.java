package com.sudo.railo.support;

import static org.mockito.Mockito.*;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
@Profile("test")
public class MockMailConfig {

	@Bean
	public JavaMailSender mockJavaMailSender() {
		return mock(JavaMailSender.class);
	}
}
