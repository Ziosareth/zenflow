package it.zenflow;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
@Slf4j
class ZenflowApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void testBCryptPasswordEncoder() {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		String encoded = encoder.encode("changeme");
		log.debug("Encoded password: {}", encoded);
	}
}
