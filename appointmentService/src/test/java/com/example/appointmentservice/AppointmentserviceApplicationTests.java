package com.example.appointmentservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "services.payment.internal-api-key=test-secret")
class AppointmentserviceApplicationTests {

	@Test
	void contextLoads() {
	}

}
