package com.sudo.railo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sudo.railo.support.MockMailConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import(MockMailConfig.class)
class RailoApplicationTests {

	@Test
	void contextLoads() {
	}

}
