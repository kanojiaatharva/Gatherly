package com.gatherly;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatherly.auth.AuthResponse;
import com.gatherly.auth.RegisterRequest;
import com.gatherly.events.CreateEventRequest;
import com.gatherly.events.EventResponse;
import com.gatherly.events.UpdateEventRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventOwnershipIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken(String email, String name) throws Exception {
        RegisterRequest req = new RegisterRequest(email, "StrongPassword123!", name);
        MvcResult res = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse auth = objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class);
        return auth.token();
    }

    @Test
    void userCannotAccessOrMutateAnotherUsersEvent() throws Exception {
        String tokenUserA = registerAndGetToken("alice@example.com", "Alice");
        String tokenUserB = registerAndGetToken("bob@example.com", "Bob");

        // Alice creates an event
        CreateEventRequest createReq = new CreateEventRequest(
                "Alice's 30th Birthday",
                "Birthday",
                LocalDate.now().plusMonths(2),
                "Botanical Gardens",
                50,
                "INR",
                new BigDecimal("75000.00"),
                "Botanical elegance",
                "Vegetarian friendly",
                "Wheelchair ramp",
                "Great music and photography"
        );

        MvcResult createResult = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice's 30th Birthday"))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.totalBudget").value(75000.00))
                .andReturn();

        EventResponse createdEvent = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), EventResponse.class);
        Long eventId = createdEvent.id();
        assertNotNull(eventId);

        // Alice can view her event
        mockMvc.perform(get("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId));

        // Bob tries to view Alice's event -> 403 Forbidden
        mockMvc.perform(get("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Bob tries to update Alice's event -> 403 Forbidden
        UpdateEventRequest updateReq = new UpdateEventRequest(
                "Hacked Birthday",
                null, null, null, null, null, null, null, null, null, null
        );
        mockMvc.perform(patch("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        // Bob tries to delete Alice's event -> 403 Forbidden
        mockMvc.perform(delete("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isForbidden());

        // Alice updates her event
        UpdateEventRequest aliceUpdate = new UpdateEventRequest(
                "Alice's Spectacular 30th",
                null, null, null, 60, null, new BigDecimal("80000.00"), null, null, null, null
        );
        mockMvc.perform(patch("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aliceUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice's Spectacular 30th"))
                .andExpect(jsonPath("$.guestCount").value(60))
                .andExpect(jsonPath("$.totalBudget").value(80000.00));

        // Alice deletes her event
        mockMvc.perform(delete("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isNoContent());

        // Event is now not found
        mockMvc.perform(get("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isNotFound());
    }
}
