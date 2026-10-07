package com.mfa.adaptivemfabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AdaptiveMfaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdaptiveMfaBackendApplication.class, args);
	}
}