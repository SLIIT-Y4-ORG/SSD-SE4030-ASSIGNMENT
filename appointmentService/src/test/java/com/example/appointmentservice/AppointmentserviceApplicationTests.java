package com.example.appointmentservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "services.payment.internal-api-key=test-secret")
@ActiveProfiles("test")
class AppointmentserviceApplicationTests {

	@Test
	void contextLoads() {
	}

}
