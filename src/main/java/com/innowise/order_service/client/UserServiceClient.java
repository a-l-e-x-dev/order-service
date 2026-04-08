package com.innowise.order_service.client;

import com.innowise.order_service.dto.UserDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Set;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserServiceClient {

    @GetMapping("/api/v1/users/{id}")
    @CircuitBreaker(name = "userService", fallbackMethod = "fallbackGetUserById")
    UserDto getUserById(@PathVariable("id") Long id);

    default UserDto fallbackGetUserById(Long id, Throwable throwable) {
        return new UserDto(id, "unknown@email.com", "Unknown", "User Service Unavailable");
    }

    @PostMapping("/api/v1/users/bulk")
    @CircuitBreaker(name = "userService", fallbackMethod = "fallbackGetUsersByIds")
    List<UserDto> getUsersByIds(@RequestBody Set<Long> ids);

    default List<UserDto> fallbackGetUsersByIds(Set<Long> ids, Throwable throwable) {
        return List.of();
    }
}