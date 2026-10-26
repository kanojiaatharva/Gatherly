package com.gatherly;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatherly.ai.ApplyRevisionRequest;
import com.gatherly.ai.GeneratedPlanResponse;
import com.gatherly.ai.PlanRevisionProposal;
import com.gatherly.ai.PlanRevisionRequest;
import com.gatherly.ai.PlanRevisionResponse;
import com.gatherly.auth.AuthResponse;
import com.gatherly.auth.RegisterRequest;
import com.gatherly.budget.BudgetOverviewResponse;
import com.gatherly.events.CreateEventRequest;
import com.gatherly.events.EventResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
class PlanningIntegrationTest {

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
    void testAiPlanGenerationAndRevisionFlow() throws Exception {
        String token = registerAndGetToken("weddingplanner@example.com", "Elena Vance");

        // Create wedding event
        CreateEventRequest eventReq = new CreateEventRequest(
                "Elena & Marcus Wedding",
                "Wedding",
                LocalDate.now().plusMonths(4),
                "Hilltop Vineyard",
                120,
                "INR",
                new BigDecimal("200000.00"),
                "Botanical elegance and warm candlelight",
                "Vegetarian friendly with gluten free options",
                "Step-free venue access",
                "Photography and memorable dining experience"
        );

        MvcResult eventRes = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventReq)))
                .andExpect(status().isCreated())
                .andReturn();

        EventResponse event = objectMapper.readValue(eventRes.getResponse().getContentAsString(), EventResponse.class);
        Long eventId = event.id();

        // 1. Generate Plan
        MvcResult planRes = mockMvc.perform(post("/api/events/" + eventId + "/plan/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("prompt", "Focus on fresh seasonal flowers"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").isNotEmpty())
                .andExpect(jsonPath("$.budgetItems").isArray())
                .andExpect(jsonPath("$.tasks").isArray())
                .andExpect(jsonPath("$.ideas").isArray())
                .andExpect(jsonPath("$.recommendedVendorCategories").isArray())
                .andReturn();

        GeneratedPlanResponse plan = objectMapper.readValue(
                planRes.getResponse().getContentAsString(), GeneratedPlanResponse.class);
        assertFalse(plan.budgetItems().isEmpty());
        assertFalse(plan.tasks().isEmpty());
        assertFalse(plan.ideas().isEmpty());

        // Verify budget items were persisted
        MvcResult budgetRes = mockMvc.perform(get("/api/events/" + eventId + "/budget")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItemsCount").value(plan.budgetItems().size()))
                .andReturn();

        BudgetOverviewResponse budgetOverview = objectMapper.readValue(
                budgetRes.getResponse().getContentAsString(), BudgetOverviewResponse.class);
        assertEquals(plan.budgetItems().size(), budgetOverview.items().size());

        // Verify initial revision record exists
        MvcResult revsRes = mockMvc.perform(get("/api/events/" + eventId + "/revisions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].versionNumber").value(1))
                .andReturn();

        // 2. Propose Plan Revision (Preview diff without persisting changes)
        PlanRevisionRequest revReq = new PlanRevisionRequest(
                "Cut discretionary budget by 20% and add dedicated live acoustic music"
        );

        MvcResult proposeRes = mockMvc.perform(post("/api/events/" + eventId + "/plan/revise")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changeSummary").isNotEmpty())
                .andExpect(jsonPath("$.budgetAdjustments").isArray())
                .andExpect(jsonPath("$.taskAdjustments").isArray())
                .andReturn();

        PlanRevisionProposal proposal = objectMapper.readValue(
                proposeRes.getResponse().getContentAsString(), PlanRevisionProposal.class);
        assertFalse(proposal.budgetAdjustments().isEmpty());

        // Check that revision count is STILL 1 (proposal is purely a preview)
        mockMvc.perform(get("/api/events/" + eventId + "/revisions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 3. Apply the Revision Atomically
        ApplyRevisionRequest applyReq = new ApplyRevisionRequest(proposal);

        mockMvc.perform(post("/api/events/" + eventId + "/plan/revisions/apply")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNumber").value(2))
                .andExpect(jsonPath("$.changeSummary").isNotEmpty());

        // Verify that revisions history now has 2 versions
        mockMvc.perform(get("/api/events/" + eventId + "/revisions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].versionNumber").value(2));
    }
}
