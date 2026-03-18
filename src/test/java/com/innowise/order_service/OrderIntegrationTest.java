package com.innowise.order_service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.innowise.order_service.dto.OrderRequest;
import com.innowise.order_service.entity.Item;
import com.innowise.order_service.repository.ItemRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderIntegrationTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    private ItemRepository itemRepository;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    static WireMockServer wireMockServer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("user-service.url", () -> wireMockServer.baseUrl());
    }

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        if (itemRepository.count() == 0) {
            Item item = new Item();
            item.setName("Test Laptop");
            item.setPrice(BigDecimal.valueOf(1000.00));
            itemRepository.save(item);
        }
    }

    @Test
    void shouldCreateOrderAndReturnCombinedResponse() {
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/v1/users/by-email"))
                .withQueryParam("email", WireMock.equalTo("test@mail.com"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{ \"id\": 1, \"email\": \"test@mail.com\", \"firstName\": \"Integration\", \"lastName\": \"Test\" }")));

        OrderRequest request = new OrderRequest("test@mail.com",
                List.of(new OrderRequest.OrderItemRequest(1L, 2)));

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/orders")
                .then()
                .statusCode(201) // Проверяем статус 201 Created
                .body("order.userEmail", equalTo("test@mail.com"))
                .body("order.totalPrice", equalTo(2000.0f)) // 1000 * 2
                .body("user.firstName", equalTo("Integration")); // Проверяем, что данные пришли из WireMock
    }
}