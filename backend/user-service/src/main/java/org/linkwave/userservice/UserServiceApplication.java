package org.linkwave.userservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

	@Bean
	public ObjectMapper objectMapper() {
		final var objectMapper = new ObjectMapper();
		objectMapper.findAndRegisterModules();
		return objectMapper;
	}

	@Bean
	public ModelMapper modelMapper() {
		return new ModelMapper();
	}

}
