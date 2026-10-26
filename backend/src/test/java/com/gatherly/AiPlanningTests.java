package com.gatherly;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatherly.ai.DeterministicRuleBasedPlanningProvider;
import com.gatherly.ai.GeneratedPlanResponse;
import com.gatherly.ai.OpenAiCompatiblePlanningProvider;
import com.gatherly.auth.AuthResponse;
import com.gatherly.auth.RegisterRequest;
import com.gatherly.common.AiProviderException;
import com.gatherly.events.CreateEventRequest;
import com.gatherly.events.Event;
import com.gatherly.events.EventResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
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
class AiPlanningTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeterministicRuleBasedPlanningProvider deterministicProvider;

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
    void testPlanGeneration_withMissingFields_includesFollowUpQuestionsAndAssumptions() throws Exception {
        String token = registerAndGetToken("missingfields@example.com", "Alex Chen");

        // Create an event with minimal/missing info: null date, null location, null style
        CreateEventRequest eventReq = new CreateEventRequest(
                "Alex's Casual Gathering",
                "Community event",
                null, // missing date
                null, // missing location
                50,
                "INR",
                new BigDecimal("30000.00"),
                null, // missing style
                null,
                null,
                null
        );

        MvcResult eventRes = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventReq)))
                .andExpect(status().isCreated())
                .andReturn();

        EventResponse event = objectMapper.readValue(eventRes.getResponse().getContentAsString(), EventResponse.class);
        Long eventId = event.id();

        MvcResult planRes = mockMvc.perform(post("/api/events/" + eventId + "/plan/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("prompt", ""))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followUpQuestions").isArray())
                .andExpect(jsonPath("$.assumptions").isArray())
                .andExpect(jsonPath("$.timeline").isArray())
                .andReturn();

        GeneratedPlanResponse plan = objectMapper.readValue(
                planRes.getResponse().getContentAsString(), GeneratedPlanResponse.class);

        // Verify follow up questions ask about date and location
        assertNotNull(plan.followUpQuestions());
        assertTrue(plan.followUpQuestions().size() >= 2, "Expected at least 2 follow up questions for missing date and location");
        assertTrue(plan.followUpQuestions().stream().anyMatch(q -> q.toLowerCase().contains("date") || q.toLowerCase().contains("month")));
        assertTrue(plan.followUpQuestions().stream().anyMatch(q -> q.toLowerCase().contains("city") || q.toLowerCase().contains("neighborhood")));

        // Verify assumptions document the defaults
        assertNotNull(plan.assumptions());
        assertFalse(plan.assumptions().isEmpty());
        assertTrue(plan.assumptions().stream().anyMatch(a -> a.toLowerCase().contains("date") || a.toLowerCase().contains("days")));

        // Verify phased timeline exists
        assertNotNull(plan.timeline());
        assertFalse(plan.timeline().isEmpty());
    }

    @Test
    void testPlanGeneration_overBudget_flagsOverBudgetAndTradeOffsWithoutModifyingEventBudget() throws Exception {
        String token = registerAndGetToken("budgetcheck@example.com", "David Kim");

        // Target budget is tiny (500 INR) for a 150-guest wedding
        BigDecimal originalBudget = new BigDecimal("500.00");
        CreateEventRequest eventReq = new CreateEventRequest(
                "Tiny Budget Wedding",
                "Wedding",
                LocalDate.now().plusMonths(3),
                "Grand Hall",
                150,
                "INR",
                originalBudget,
                "Luxury floral",
                "Gourmet 5-course",
                "Full accessibility",
                "Exquisite decor"
        );

        MvcResult eventRes = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventReq)))
                .andExpect(status().isCreated())
                .andReturn();

        EventResponse event = objectMapper.readValue(eventRes.getResponse().getContentAsString(), EventResponse.class);
        Long eventId = event.id();

        MvcResult planRes = mockMvc.perform(post("/api/events/" + eventId + "/plan/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("prompt", ""))))
                .andExpect(status().isOk())
                .andReturn();

        GeneratedPlanResponse plan = objectMapper.readValue(
                planRes.getResponse().getContentAsString(), GeneratedPlanResponse.class);

        // 1. Backend computes total with BigDecimal
        assertNotNull(plan.totalCalculatedBudget());
        assertTrue(plan.totalCalculatedBudget().compareTo(originalBudget) > 0);

        // 2. Over-budget is flagged and warning & trade-offs are populated
        assertTrue(plan.isOverBudget());
        assertNotNull(plan.budgetWarning());
        assertTrue(plan.budgetWarning().contains("exceed your target budget"));
        assertNotNull(plan.tradeOffSuggestions());
        assertFalse(plan.tradeOffSuggestions().isEmpty());

        // 3. The user's target budget is NEVER silently changed
        MvcResult getEventRes = mockMvc.perform(get("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        EventResponse persistedEvent = objectMapper.readValue(getEventRes.getResponse().getContentAsString(), EventResponse.class);
        assertEquals(0, originalBudget.compareTo(persistedEvent.totalBudget()), "Event total budget must NOT be silently modified");
    }

    @Test
    void testPlanGeneration_developmentMockMode_isExplicitlyLabeled() throws Exception {
        String token = registerAndGetToken("mockmode@example.com", "Sarah Connor");

        CreateEventRequest eventReq = new CreateEventRequest(
                "Sarah's Birthday",
                "Birthday",
                LocalDate.now().plusDays(40),
                "Downtown Bistro",
                40,
                "INR",
                new BigDecimal("60000.00"),
                "Retro Disco",
                "Nut-free cakes",
                "Ground floor seating",
                "Great music"
        );

        MvcResult eventRes = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long eventId = objectMapper.readValue(eventRes.getResponse().getContentAsString(), EventResponse.class).id();

        MvcResult planRes = mockMvc.perform(post("/api/events/" + eventId + "/plan/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("prompt", ""))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerMode").value("DEVELOPMENT_MOCK"))
                .andExpect(jsonPath("$.providerNotice").isNotEmpty())
                .andReturn();

        GeneratedPlanResponse plan = objectMapper.readValue(
                planRes.getResponse().getContentAsString(), GeneratedPlanResponse.class);

        assertEquals("DEVELOPMENT_MOCK", plan.providerMode());
        assertTrue(plan.providerNotice().contains("DEVELOPMENT MOCK"));
    }

    @Test
    void testOpenAiCompatibleProvider_liveConfigured_withInvalidBaseUrl_throwsAiProviderException() {
        // Test unit behavior of live provider when provider endpoint fails
        OpenAiCompatiblePlanningProvider liveProvider = new OpenAiCompatiblePlanningProvider(
                "openai",
                "sk-test-live-key",
                "gpt-4o-mini",
                "http://127.0.0.1:54321/invalid-v1", // invalid port
                5,
                deterministicProvider,
                objectMapper
        );

        Event testEvent = new Event();
        testEvent.setName("Live Test Event");
        testEvent.setEventType("Birthday");
        testEvent.setTotalBudget(new BigDecimal("50000.00"));
        testEvent.setCurrency("INR");
        testEvent.setGuestCount(30);

        assertThrows(AiProviderException.class, () -> {
            liveProvider.generatePlan(testEvent, "Test note");
        });
    }

    @Test
    void testRateLimiting_returns429WhenLimitExceeded() throws Exception {
        String token = registerAndGetToken("ratelimit@example.com", "Speedy Gonzales");

        CreateEventRequest eventReq = new CreateEventRequest(
                "Speedy Event",
                "Birthday",
                LocalDate.now().plusDays(30),
                "Party Lounge",
                25,
                "INR",
                new BigDecimal("25000.00"),
                null, null, null, null
        );

        MvcResult eventRes = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long eventId = objectMapper.readValue(eventRes.getResponse().getContentAsString(), EventResponse.class).id();

        // Perform requests up to the rate limit (15)
        boolean hit429 = false;
        for (int i = 0; i < 20; i++) {
            MvcResult res = mockMvc.perform(post("/api/events/" + eventId + "/plan/generate")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("prompt", "Attempt " + i))))
                    .andReturn();

            if (res.getResponse().getStatus() == 429) {
                hit429 = true;
                break;
            }
        }

        assertTrue(hit429, "Expected rate limit (HTTP 429) to be triggered when making rapid requests");
    }
}
