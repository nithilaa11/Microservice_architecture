package com.ecommerce.order.client;

import com.ecommerce.order.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OpenFeign Client for synchronous REST communication with User Service.
 * Resolves the service instance dynamically via Eureka Service Discovery (USER-SERVICE).
 */
@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/users/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);
}
