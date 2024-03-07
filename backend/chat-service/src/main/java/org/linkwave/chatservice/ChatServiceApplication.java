package org.linkwave.chatservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.linkwave.shared.storage.FileStorageService;
import org.linkwave.shared.storage.StorageService;
import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

@SpringBootApplication
@EnableFeignClients
public class ChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
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

    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory databaseFactory)  {
        return new MongoTransactionManager(databaseFactory);
    }

    @Bean
    public StorageService storageService() {
        return new FileStorageService();
    }

}
