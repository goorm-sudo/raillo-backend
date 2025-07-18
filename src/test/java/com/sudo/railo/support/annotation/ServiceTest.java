package com.sudo.railo.support.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sudo.railo.support.extension.DatabaseCleanupExtension;
import com.sudo.railo.support.extension.RedisCleanupExtension;
import com.sudo.railo.support.extension.RedisServerExtension;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith({RedisServerExtension.class, DatabaseCleanupExtension.class, RedisCleanupExtension.class})
public @interface ServiceTest {
}
