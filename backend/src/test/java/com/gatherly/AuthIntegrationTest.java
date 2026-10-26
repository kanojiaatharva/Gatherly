package com.gatherly;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatherly.auth.AuthResponse;
import com.gatherly.auth.LoginRequest;
import com.gatherly.auth.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.gatherly.common.RateLimiterService rateLimiterService;

    @Test
    void registerAndLoginFlow() throws Exception {
        RegisterRequest registerReq = new RegisterRequest(
                "sarah@example.com",
                "Password123!",
                "Sarah Miller"
        );

        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.email").value("sarah@example.com"))
                .andExpect(jsonPath("$.user.displayName").value("Sarah Miller"))
                .andReturn();

        AuthResponse regAuth = objectMapper.readValue(
                regResult.getResponse().getContentAsString(), AuthResponse.class);
        assertNotNull(regAuth.token());
        assertFalse(regAuth.token().isBlank());

        // Duplicate registration should fail
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));

        // Login with correct password
        LoginRequest loginReq = new LoginRequest("sarah@example.com", "Password123!");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn();

        AuthResponse loginAuth = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), AuthResponse.class);

        // Access /api/auth/me with token
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + loginAuth.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("sarah@example.com"));

        // Access /api/auth/me without token -> 401
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        // Login with bad password -> 400
        LoginRequest badLogin = new LoginRequest("sarah@example.com", "WrongPassword");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authRateLimitBlocksFlooding() throws Exception {
        com.gatherly.common.RateLimiterService limiter = rateLimiterService;
        String floodIp = "192.168.100.99";
        for (int i = 0; i < 60; i++) {
            limiter.checkLimit("auth:" + floodIp, 60, "Too many authentication attempts.");
        }

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", floodIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("attacker@example.com", "anyPass123!"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"));

        limiter.clear("auth:" + floodIp);
    }
}
