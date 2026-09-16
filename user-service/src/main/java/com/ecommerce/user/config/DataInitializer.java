package com.ecommerce.user.config;

import com.ecommerce.user.model.User;
import com.ecommerce.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initUserData(UserRepository userRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                List<User> sampleUsers = List.of(
                        new User(null, "Alice Johnson", "alice@example.com", "9876543210"),
                        new User(null, "Bob Smith", "bob@example.com", "9876543211"),
                        new User(null, "Charlie Brown", "charlie@example.com", "9876543212")
                );
                userRepository.saveAll(sampleUsers);
                log.info("Successfully initialized {} sample users in user-service database.", sampleUsers.size());
            }
        };
    }
}
