package com.gatherly;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatherly.auth.AuthResponse;
import com.gatherly.auth.RegisterRequest;
import com.gatherly.events.CreateEventRequest;
import com.gatherly.events.UpdateEventRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        String uniqueEmail = "val_" + System.currentTimeMillis() + "@example.com";
        RegisterRequest req = new RegisterRequest(uniqueEmail, "StrongPassword123!", "Validation Tester");
        MvcResult res = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse auth = objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class);
        token = auth.token();
    }

    @Test
    void healthEndpointWorksWithoutAuth() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("gatherly-api"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("gatherly-api"));
    }

    @Test
    void rejectMissingEventName() throws Exception {
        CreateEventRequest req = new CreateEventRequest(
                "", // Blank name
                "Birthday",
                LocalDate.now().plusDays(30),
                "City Park",
                25,
                "INR",
                new BigDecimal("10000.00"),
                null, null, null, null
        );

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").isNotEmpty());
    }

    @Test
    void rejectMissingEventType() throws Exception {
        CreateEventRequest req = new CreateEventRequest(
                "Annual Dinner",
                "   ", // Blank type
                LocalDate.now().plusDays(30),
                "City Park",
                25,
                "INR",
                new BigDecimal("10000.00"),
                null, null, null, null
        );

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.eventType").isNotEmpty());
    }

    @Test
    void rejectNegativeBudget() throws Exception {
        CreateEventRequest req = new CreateEventRequest(
                "Deficit Party",
                "Birthday",
                LocalDate.now().plusDays(30),
                "City Park",
                25,
                "INR",
                new BigDecimal("-500.00"), // Negative budget
                null, null, null, null
        );

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.totalBudget").isNotEmpty());
    }

    @Test
    void rejectInvalidGuestCount() throws Exception {
        CreateEventRequest req = new CreateEventRequest(
                "Ghost Party",
                "Birthday",
                LocalDate.now().plusDays(30),
                "City Park",
                0, // 0 guests
                "INR",
                new BigDecimal("5000.00"),
                null, null, null, null
        );

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.guestCount").isNotEmpty());
    }

    @Test
    void rejectInvalidCurrencyCode() throws Exception {
        CreateEventRequest req = new CreateEventRequest(
                "Forex Party",
                "Birthday",
                LocalDate.now().plusDays(30),
                "City Park",
                50,
                "TOOLONG", // Invalid currency code
                new BigDecimal("5000.00"),
                null, null, null, null
        );

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.currency").isNotEmpty());
    }

    @Test
    void rejectNegativeBudgetOnPatch() throws Exception {
        // First create valid event
        CreateEventRequest validReq = new CreateEventRequest(
                "Graduation Celebration",
                "Graduation",
                LocalDate.now().plusDays(60),
                "Campus Lawn",
                40,
                "USD",
                new BigDecimal("3000.00"),
                null, null, null, null
        );

        MvcResult createRes = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long eventId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // Update with negative budget
        UpdateEventRequest invalidUpdate = new UpdateEventRequest(
                null, null, null, null, null, null, new BigDecimal("-200.00"), null, null, null, null
        );

        mockMvc.perform(patch("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.totalBudget").isNotEmpty());
    }
}
