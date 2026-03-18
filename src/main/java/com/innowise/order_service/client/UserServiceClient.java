package com.innowise.order_service.client;

import com.innowise.order_service.dto.UserDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserServiceClient {
    @GetMapping("/api/v1/users/by-email")
    @CircuitBreaker(name = "userService", fallbackMethod = "fallbackGetUserByEmail")
    UserDto getUserByEmail(@RequestParam("email") String email);

    default UserDto fallbackGetUserByEmail(String email, Throwable throwable) {
        return new UserDto(null, email, "Unknown", "User Service Unavailable");
    }
}