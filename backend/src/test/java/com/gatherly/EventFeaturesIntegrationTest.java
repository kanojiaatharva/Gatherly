package com.gatherly;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatherly.auth.AuthResponse;
import com.gatherly.auth.RegisterRequest;
import com.gatherly.budget.BudgetOverviewResponse;
import com.gatherly.budget.CreateBudgetItemRequest;
import com.gatherly.events.CreateEventRequest;
import com.gatherly.events.EventResponse;
import com.gatherly.tasks.CreateTaskRequest;
import com.gatherly.tasks.TaskOverviewResponse;
import com.gatherly.tasks.TaskResponse;
import com.gatherly.vendors.CreateVendorRequest;
import com.gatherly.vendors.VendorResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventFeaturesIntegrationTest {

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
    void testBudgetTasksAndVendorsFlow() throws Exception {
        String token = registerAndGetToken("planner@example.com", "Expert Planner");

        // Create an Event with budget 50,000 INR
        CreateEventRequest eventReq = new CreateEventRequest(
                "Silver Jubilee Anniversary",
                "Anniversary",
                LocalDate.now().plusDays(45),
                "Grand Hall",
                100,
                "INR",
                new BigDecimal("50000.00"),
                "Classic silver and white",
                "Vegetarian & Jain options",
                "Elevator access",
                "Memorable dinner and speeches"
        );

        MvcResult eventRes = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventReq)))
                .andExpect(status().isCreated())
                .andReturn();

        EventResponse event = objectMapper.readValue(eventRes.getResponse().getContentAsString(), EventResponse.class);
        Long eventId = event.id();

        // 1. Add Budget Items
        CreateBudgetItemRequest item1 = new CreateBudgetItemRequest(
                "Catering",
                "Buffet dinner for 100 guests",
                new BigDecimal("30000.00"),
                null,
                true
        );
        CreateBudgetItemRequest item2 = new CreateBudgetItemRequest(
                "Venue",
                "Hall rental deposit",
                new BigDecimal("15000.00"),
                new BigDecimal("15000.00"),
                false
        );
        CreateBudgetItemRequest item3 = new CreateBudgetItemRequest(
                "Photography",
                "Event photographer 4 hours",
                new BigDecimal("10000.00"),
                null,
                true
        );

        for (CreateBudgetItemRequest item : List.of(item1, item2, item3)) {
            mockMvc.perform(post("/api/events/" + eventId + "/budget-items")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(item)))
                    .andExpect(status().isCreated());
        }

        // Verify Server-calculated Budget Overview:
        // Total budget: 50,000. Total estimated: 30,000 + 15,000 + 10,000 = 55,000.
        // isOverBudget should be TRUE!
        MvcResult budgetRes = mockMvc.perform(get("/api/events/" + eventId + "/budget")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBudget").value(50000.00))
                .andExpect(jsonPath("$.totalEstimated").value(55000.00))
                .andExpect(jsonPath("$.totalActual").value(15000.00))
                .andExpect(jsonPath("$.remainingBudget").value(-5000.00))
                .andExpect(jsonPath("$.isOverBudget").value(true))
                .andExpect(jsonPath("$.estimateCount").value(2))
                .andExpect(jsonPath("$.confirmedCount").value(1))
                .andReturn();

        BudgetOverviewResponse budget = objectMapper.readValue(
                budgetRes.getResponse().getContentAsString(), BudgetOverviewResponse.class);
        assertEquals(3, budget.categories().size());

        // 2. Add Tasks and verify overdue logic
        CreateTaskRequest overdueTask = new CreateTaskRequest(
                "Send save-the-dates",
                "Send email invitations to family",
                LocalDate.now().minusDays(5),
                "TODO",
                "HIGH",
                "Sarah"
        );
        CreateTaskRequest futureTask = new CreateTaskRequest(
                "Finalize menu tasting",
                "Select vegetarian entrees",
                LocalDate.now().plusDays(10),
                "TODO",
                "MEDIUM",
                "Alex"
        );

        mockMvc.perform(post("/api/events/" + eventId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overdueTask)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isOverdue").value(true))
                .andExpect(jsonPath("$.suggestedAlternativeDate").isNotEmpty());

        mockMvc.perform(post("/api/events/" + eventId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(futureTask)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isOverdue").value(false));

        MvcResult taskOverviewRes = mockMvc.perform(get("/api/events/" + eventId + "/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(2))
                .andExpect(jsonPath("$.overdueTasks").value(1))
                .andReturn();

        TaskOverviewResponse taskOverview = objectMapper.readValue(
                taskOverviewRes.getResponse().getContentAsString(), TaskOverviewResponse.class);
        assertEquals(2, taskOverview.tasks().size());

        // 3. Add Vendor
        CreateVendorRequest vendorReq = new CreateVendorRequest(
                "Photography",
                "Moments Studio",
                "hello@momentsstudio.test, +91 9876543210",
                new BigDecimal("10000.00"),
                "QUOTED",
                "SHORTLISTED",
                "Sent portfolio, awaiting confirmation"
        );

        mockMvc.perform(post("/api/events/" + eventId + "/vendors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendorReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("Photography"))
                .andExpect(jsonPath("$.quoteStatus").value("QUOTED"))
                .andExpect(jsonPath("$.bookingStatus").value("SHORTLISTED"));

        mockMvc.perform(get("/api/events/" + eventId + "/vendors")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
