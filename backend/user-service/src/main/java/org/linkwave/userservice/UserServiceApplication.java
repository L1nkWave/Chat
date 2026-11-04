package org.linkwave.userservice;

import tools.jackson.databind.ObjectMapper;
import org.linkwave.shared.storage.LocalFileStorageService;
import org.linkwave.shared.storage.FileStorageService;
import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

	@Bean
	public ObjectMapper objectMapper() {
		final var objectMapper = new ObjectMapper();
		return objectMapper;
	}

	@Bean
	public ModelMapper modelMapper() {
		return new ModelMapper();
	}

	@Bean
	public FileStorageService storageService() {
		return new LocalFileStorageService();
	}

}