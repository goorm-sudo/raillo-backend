package com.sudo.railo.member.application;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import com.sudo.railo.support.annotation.ServiceTest;

@ServiceTest
class MemberNoGeneratorTest {

	@Autowired
	private MemberNoGenerator memberNoGenerator;

	@Autowired
	private RedisTemplate<String, String> stringRedisTemplate;

	@Test
	@DisplayName("회원번호 생성에 성공한다.")
	void generateMemberNo_success() {
		//given
		String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String redisKey = "todayKey:" + today;

		//when
		String memberNo1 = memberNoGenerator.generateMemberNo();
		String memberNo2 = memberNoGenerator.generateMemberNo();

		//then
		assertThat(memberNo1).isEqualTo(today + "0001");
		assertThat(memberNo2).isEqualTo(today + "0002");

		String counterValue = stringRedisTemplate.opsForValue().get(redisKey);
		assertThat(counterValue).isEqualTo("2");
	}

}
