package com.gatherly;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatherly.auth.AuthResponse;
import com.gatherly.auth.RegisterRequest;
import com.gatherly.budget.BudgetItemResponse;
import com.gatherly.budget.BudgetOverviewResponse;
import com.gatherly.budget.CreateBudgetItemRequest;
import com.gatherly.budget.UpdateBudgetItemRequest;
import com.gatherly.events.CreateEventRequest;
import com.gatherly.events.EventResponse;
import com.gatherly.events.ImpactAnalysisRequest;
import com.gatherly.events.ImpactAnalysisResponse;
import com.gatherly.tasks.CreateTaskRequest;
import com.gatherly.tasks.TaskResponse;
import com.gatherly.tasks.UpdateTaskRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class BudgetAndTaskTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private Long eventId;

    @BeforeEach
    void setUp() throws Exception {
        String email = "phase3_user_" + System.currentTimeMillis() + "@gatherly.test";
        RegisterRequest registerRequest = new RegisterRequest(email, "StrongSecret123!", "Phase 3 Planner");

        MvcResult regRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(regRes.getResponse().getContentAsString(), AuthResponse.class);
        userToken = "Bearer " + auth.token();

        CreateEventRequest eventReq = new CreateEventRequest(
                "Silver Jubilee Anniversary",
                "ANNIVERSARY",
                LocalDate.now().plusDays(60),
                "Mumbai, India",
                100,
                "INR",
                new BigDecimal("500000.00"),
                "Elegantly Classic",
                "Vegetarian options required",
                "Wheelchair access needed",
                "Catering, Photography"
        );

        MvcResult evRes = mockMvc.perform(post("/api/events")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventReq)))
                .andExpect(status().isCreated())
                .andReturn();

        EventResponse event = objectMapper.readValue(evRes.getResponse().getContentAsString(), EventResponse.class);
        eventId = event.id();
    }

    @Test
    @DisplayName("Budget CRUD: calculates BigDecimal totals, remaining balance, and confirms actuals")
    void testBudgetCrudAndCalculations() throws Exception {
        // 1. Add estimated item
        CreateBudgetItemRequest item1 = new CreateBudgetItemRequest(
                "Catering & Bar",
                "Dinner buffet for 100 guests",
                new BigDecimal("250000.00"),
                null,
                true
        );

        MvcResult addRes = mockMvc.perform(post("/api/events/" + eventId + "/budget-items")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item1)))
                .andExpect(status().isCreated())
                .andReturn();

        BudgetItemResponse added1 = objectMapper.readValue(addRes.getResponse().getContentAsString(), BudgetItemResponse.class);
        assertThat(added1.isEstimate()).isTrue();
        assertThat(added1.estimatedAmount()).isEqualByComparingTo("250000.00");

        // 2. Add second item
        CreateBudgetItemRequest item2 = new CreateBudgetItemRequest(
                "Photography",
                "Candid & stage coverage",
                new BigDecimal("80000.00"),
                null,
                true
        );

        mockMvc.perform(post("/api/events/" + eventId + "/budget-items")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item2)))
                .andExpect(status().isCreated());

        // 3. Verify budget overview: total estimated = 330000, remaining = 170000, not over budget
        MvcResult bgRes = mockMvc.perform(get("/api/events/" + eventId + "/budget")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andReturn();

        BudgetOverviewResponse overview = objectMapper.readValue(bgRes.getResponse().getContentAsString(), BudgetOverviewResponse.class);
        assertThat(overview.totalEstimated()).isEqualByComparingTo("330000.00");
        assertThat(overview.remainingBudget()).isEqualByComparingTo("170000.00");
        assertThat(overview.isOverBudget()).isFalse();

        // 4. Update item 1 with actual amount -> auto confirms estimate to false
        UpdateBudgetItemRequest updateItem1 = new UpdateBudgetItemRequest(
                "Catering & Bar",
                "Final catering contract signed",
                new BigDecimal("260000.00"),
                new BigDecimal("260000.00"),
                null
        );

        MvcResult updateRes = mockMvc.perform(patch("/api/events/" + eventId + "/budget-items/" + added1.id())
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateItem1)))
                .andExpect(status().isOk())
                .andReturn();

        BudgetItemResponse updated1 = objectMapper.readValue(updateRes.getResponse().getContentAsString(), BudgetItemResponse.class);
        assertThat(updated1.actualAmount()).isEqualByComparingTo("260000.00");
        assertThat(updated1.isEstimate()).isFalse();

        // 5. Delete item 2
        mockMvc.perform(delete("/api/events/" + eventId + "/budget-items/" + added1.id())
                        .header("Authorization", userToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Budget validation: rejects negative amounts")
    void testBudgetRejectsNegativeAmounts() throws Exception {
        CreateBudgetItemRequest negativeItem = new CreateBudgetItemRequest(
                "Decor",
                "Invalid negative cost",
                new BigDecimal("-500.00"),
                null,
                true
        );

        mockMvc.perform(post("/api/events/" + eventId + "/budget-items")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeItem)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Task dependencies: links prerequisite, enforces cycle prevention, and flags blocked state")
    void testTaskDependenciesAndBlocking() throws Exception {
        // 1. Create Prerequisite Task: "Finalize Guest List"
        CreateTaskRequest req1 = new CreateTaskRequest(
                "Finalize Guest List",
                "Collect all addresses and confirmed attendees",
                LocalDate.now().plusDays(10),
                "TODO",
                "HIGH",
                "Host"
        );

        MvcResult res1 = mockMvc.perform(post("/api/events/" + eventId + "/tasks")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn();

        TaskResponse task1 = objectMapper.readValue(res1.getResponse().getContentAsString(), TaskResponse.class);

        // 2. Create Dependent Task: "Send Printed Invitations", depending on task 1
        CreateTaskRequest req2 = new CreateTaskRequest(
                "Send Printed Invitations",
                "Mail invitations to finalized addresses",
                LocalDate.now().plusDays(20),
                "TODO",
                "HIGH",
                "Host",
                task1.id()
        );

        MvcResult res2 = mockMvc.perform(post("/api/events/" + eventId + "/tasks")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andReturn();

        TaskResponse task2 = objectMapper.readValue(res2.getResponse().getContentAsString(), TaskResponse.class);
        assertThat(task2.dependsOnTaskId()).isEqualTo(task1.id());
        assertThat(task2.isBlocked()).isTrue(); // Task 1 is TODO, so Task 2 is blocked

        // 3. Verify Self-dependency prevention
        UpdateTaskRequest selfDepReq = new UpdateTaskRequest(
                null, null, null, null, null, null, task1.id()
        );
        mockMvc.perform(patch("/api/events/" + eventId + "/tasks/" + task1.id())
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selfDepReq)))
                .andExpect(status().isBadRequest());

        // 4. Verify Circular dependency prevention: Task 1 cannot depend on Task 2
        UpdateTaskRequest circularReq = new UpdateTaskRequest(
                null, null, null, null, null, null, task2.id()
        );
        mockMvc.perform(patch("/api/events/" + eventId + "/tasks/" + task1.id())
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(circularReq)))
                .andExpect(status().isBadRequest());

        // 5. Complete Task 1 -> Task 2 should unblock
        UpdateTaskRequest completeTask1 = new UpdateTaskRequest(
                null, null, null, "COMPLETED", null, null
        );
        mockMvc.perform(patch("/api/events/" + eventId + "/tasks/" + task1.id())
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeTask1)))
                .andExpect(status().isOk());

        MvcResult getTask2Res = mockMvc.perform(get("/api/events/" + eventId + "/tasks")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(getTask2Res.getResponse().getContentAsString()).contains("\"isBlocked\":false");
    }

    @Test
    @DisplayName("Impact Analysis: calculates variable guest impacts, timeline shifts, and budget overages")
    void testImpactAnalysis() throws Exception {
        // Add a catering budget item
        CreateBudgetItemRequest catering = new CreateBudgetItemRequest(
                "Catering Buffet",
                "Dinner per guest",
                new BigDecimal("200000.00"),
                null,
                true
        );
        mockMvc.perform(post("/api/events/" + eventId + "/budget-items")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catering)))
                .andExpect(status().isCreated());

        // Request impact analysis: increase guests from 100 to 150, reduce budget to 180,000, shift date earlier by 30 days
        ImpactAnalysisRequest analysisReq = new ImpactAnalysisRequest(
                150,
                LocalDate.now().plusDays(30),
                new BigDecimal("180000.00")
        );

        MvcResult res = mockMvc.perform(post("/api/events/" + eventId + "/impact-analysis")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(analysisReq)))
                .andExpect(status().isOk())
                .andReturn();

        ImpactAnalysisResponse response = objectMapper.readValue(res.getResponse().getContentAsString(), ImpactAnalysisResponse.class);

        // Budget impact assertions
        assertThat(response.budgetImpact().isProjectedOverBudget()).isTrue();
        assertThat(response.budgetImpact().proposedBudget()).isEqualByComparingTo("180000.00");
        assertThat(response.budgetImpact().categoryImpacts()).isNotEmpty();

        // Timeline impact assertions: schedule compressed by ~30 days
        assertThat(response.timelineImpact().isScheduleCompressed()).isTrue();
        assertThat(response.timelineImpact().daysShifted()).isLessThan(0);

        // Recommendations returned
        assertThat(response.recommendations()).isNotEmpty();
    }
}
