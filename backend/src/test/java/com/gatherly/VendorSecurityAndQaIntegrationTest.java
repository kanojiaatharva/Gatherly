package com.gatherly;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatherly.auth.AuthResponse;
import com.gatherly.auth.RegisterRequest;
import com.gatherly.budget.CreateBudgetItemRequest;
import com.gatherly.common.RateLimiterService;
import com.gatherly.events.CreateEventRequest;
import com.gatherly.events.EventResponse;
import com.gatherly.events.ImpactAnalysisRequest;
import com.gatherly.tasks.CreateTaskRequest;
import com.gatherly.vendors.CreateVendorRequest;
import com.gatherly.vendors.UpdateVendorRequest;
import com.gatherly.vendors.VendorResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VendorSecurityAndQaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RateLimiterService rateLimiterService;

    private String aliceToken;
    private Long aliceEventId;
    private String bobToken;

    @BeforeEach
    void setUp() throws Exception {
        String uniqueSuffix = System.currentTimeMillis() + "_" + Math.round(Math.random() * 1000);
        aliceToken = registerAndGetToken("alice_qa_" + uniqueSuffix + "@example.com", "Alice QA");
        bobToken = registerAndGetToken("bob_qa_" + uniqueSuffix + "@example.com", "Bob QA");

        CreateEventRequest createReq = new CreateEventRequest(
                "Alice's Gala",
                "Community Gala",
                LocalDate.now().plusMonths(3),
                "Grand Ballroom",
                120,
                "USD",
                new BigDecimal("25000.00"),
                "Modern Glam",
                "Halal & Vegan Options",
                "Step-free access",
                "Live band & verified vendors"
        );

        MvcResult res = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        EventResponse created = objectMapper.readValue(res.getResponse().getContentAsString(), EventResponse.class);
        aliceEventId = created.id();
    }

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
    void crossUserAccessBlockedAcrossAllSubResources() throws Exception {
        // Bob tries to GET Alice's vendors -> 403 Forbidden
        mockMvc.perform(get("/api/events/" + aliceEventId + "/vendors")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        // Bob tries to POST a vendor to Alice's event -> 403 Forbidden
        CreateVendorRequest vendorReq = new CreateVendorRequest(
                "Florals", "Infiltrator Flowers", "hacked@example.com",
                new BigDecimal("500.00"), "UNVERIFIED", "NOT_BOOKED", "Test"
        );
        mockMvc.perform(post("/api/events/" + aliceEventId + "/vendors")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendorReq)))
                .andExpect(status().isForbidden());

        // Bob tries to GET Alice's budget -> 403 Forbidden
        mockMvc.perform(get("/api/events/" + aliceEventId + "/budget")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());

        // Bob tries to POST a budget item to Alice's event -> 403 Forbidden
        CreateBudgetItemRequest budgetReq = new CreateBudgetItemRequest(
                "Security", "Private Guard", new BigDecimal("1000.00"), null, true
        );
        mockMvc.perform(post("/api/events/" + aliceEventId + "/budget-items")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(budgetReq)))
                .andExpect(status().isForbidden());

        // Bob tries to GET Alice's tasks -> 403 Forbidden
        mockMvc.perform(get("/api/events/" + aliceEventId + "/tasks")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());

        // Bob tries to trigger AI plan generation for Alice's event -> 403 Forbidden
        mockMvc.perform(post("/api/events/" + aliceEventId + "/plan/generate")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());

        // Bob tries to trigger Impact Analysis on Alice's event -> 403 Forbidden
        ImpactAnalysisRequest impactReq = new ImpactAnalysisRequest(150, null, null);
        mockMvc.perform(post("/api/events/" + aliceEventId + "/impact-analysis")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(impactReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    void vendorValidationRejectsNegativeAmountsAndInvalidEnums() throws Exception {
        // Negative amount should fail with 400 Bad Request
        CreateVendorRequest negAmountReq = new CreateVendorRequest(
                "Photography", "Studio One", "info@studio.com",
                new BigDecimal("-200.00"), "UNVERIFIED", "NOT_BOOKED", "Negative"
        );
        mockMvc.perform(post("/api/events/" + aliceEventId + "/vendors")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negAmountReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.quotedAmount").exists());

        // Blank category should fail with 400 Bad Request
        CreateVendorRequest blankCatReq = new CreateVendorRequest(
                "", "Studio One", "info@studio.com",
                new BigDecimal("200.00"), "UNVERIFIED", "NOT_BOOKED", "Blank Cat"
        );
        mockMvc.perform(post("/api/events/" + aliceEventId + "/vendors")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankCatReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.category").exists());

        // Invalid quote status should fail with 400 Bad Request
        CreateVendorRequest invalidStatusReq = new CreateVendorRequest(
                "Florals", "Bloom Co", "bloom@co.com",
                new BigDecimal("500.00"), "NON_EXISTENT_STATUS", "NOT_BOOKED", "Invalid Status"
        );
        mockMvc.perform(post("/api/events/" + aliceEventId + "/vendors")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidStatusReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.quoteStatus").exists());
    }

    @Test
    void vendorPersistenceAndSourceVerificationFields() throws Exception {
        Instant verificationTimestamp = Instant.now().minusSeconds(3600);
        CreateVendorRequest createReq = new CreateVendorRequest(
                "Sound & Lighting",
                "Acoustic Vibrations Ltd",
                "+1-555-0199",
                new BigDecimal("3500.00"),
                "QUOTED",
                "SHORTLISTED",
                "Includes sound technician for 6 hours",
                "DIRECT_PORTFOLIO",
                verificationTimestamp
        );

        MvcResult createRes = mockMvc.perform(post("/api/events/" + aliceEventId + "/vendors")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Acoustic Vibrations Ltd"))
                .andExpect(jsonPath("$.category").value("Sound & Lighting"))
                .andExpect(jsonPath("$.source").value("DIRECT_PORTFOLIO"))
                .andExpect(jsonPath("$.quoteStatus").value("QUOTED"))
                .andExpect(jsonPath("$.bookingStatus").value("SHORTLISTED"))
                .andExpect(jsonPath("$.lastVerifiedAt").isNotEmpty())
                .andReturn();

        VendorResponse response = objectMapper.readValue(createRes.getResponse().getContentAsString(), VendorResponse.class);
        Long vendorId = response.id();

        // Update vendor and verify audit fields
        UpdateVendorRequest updateReq = new UpdateVendorRequest(
                null, null, "+1-555-9999", new BigDecimal("3200.00"),
                "QUOTED", "BOOKED", "Discounted negotiated price",
                "CONTRACT_AGREEMENT", null
        );

        mockMvc.perform(patch("/api/events/" + aliceEventId + "/vendors/" + vendorId)
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quotedAmount").value(3200.00))
                .andExpect(jsonPath("$.bookingStatus").value("BOOKED"))
                .andExpect(jsonPath("$.source").value("CONTRACT_AGREEMENT"));

        // Query adapter status endpoint
        mockMvc.perform(get("/api/events/" + aliceEventId + "/vendors/search-adapter-status")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adapterAvailable").value(false))
                .andExpect(jsonPath("$.adapterName").isNotEmpty());
    }

    @Test
    void vendorBookingAndLinkingToBudgetActuals() throws Exception {
        // 1. Create a budget item for "Florals & Decor"
        CreateBudgetItemRequest budgetReq = new CreateBudgetItemRequest(
                "Florals & Decor",
                "Table centerpieces and entrance arch (Estimate)",
                new BigDecimal("4500.00"),
                null,
                true
        );

        mockMvc.perform(post("/api/events/" + aliceEventId + "/budget-items")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(budgetReq)))
                .andExpect(status().isCreated());

        // 2. Create a vendor quote in the same category
        CreateVendorRequest vendorReq = new CreateVendorRequest(
                "Florals & Decor",
                "Petals & Stems Studio",
                "hello@petalsandstems.com",
                new BigDecimal("4200.00"),
                "QUOTED",
                "NOT_BOOKED",
                "Official quote received with seasonal foliage discount",
                "DIRECT_PROPOSAL",
                Instant.now()
        );

        MvcResult vendorRes = mockMvc.perform(post("/api/events/" + aliceEventId + "/vendors")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendorReq)))
                .andExpect(status().isCreated())
                .andReturn();

        VendorResponse vendor = objectMapper.readValue(vendorRes.getResponse().getContentAsString(), VendorResponse.class);

        // 3. Alice books and links this vendor to the budget atomically
        mockMvc.perform(post("/api/events/" + aliceEventId + "/vendors/" + vendor.id() + "/book")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatus").value("BOOKED"))
                .andExpect(jsonPath("$.quoteStatus").value("QUOTED"));

        // 4. Verify budget now reflects confirmed actual amount 4200.00 and isEstimate = false
        mockMvc.perform(get("/api/events/" + aliceEventId + "/budget")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActual", notNullValue()))
                .andExpect(jsonPath("$.items[?(@.category == 'Florals & Decor')].actualAmount").value(hasItem(4200.00)))
                .andExpect(jsonPath("$.items[?(@.category == 'Florals & Decor')].isEstimate").value(hasItem(false)));
    }

    @Test
    void taskDatesAndChronologicalOrdering() throws Exception {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate nextWeek = LocalDate.now().plusDays(7);
        LocalDate nextMonth = LocalDate.now().plusMonths(1);

        CreateTaskRequest overdueTaskReq = new CreateTaskRequest(
                "Sign venue deposit contract", "Venue deposit was due yesterday",
                yesterday, "TODO", "URGENT", "Alice", null
        );
        CreateTaskRequest nextWeekTaskReq = new CreateTaskRequest(
                "Finalize playlist", "Song list for DJ",
                nextWeek, "TODO", "MEDIUM", "Alice", null
        );
        CreateTaskRequest nextMonthTaskReq = new CreateTaskRequest(
                "RSVP headcount lock", "Confirm final attendee count",
                nextMonth, "TODO", "HIGH", "Alice", null
        );

        mockMvc.perform(post("/api/events/" + aliceEventId + "/tasks")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nextMonthTaskReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/events/" + aliceEventId + "/tasks")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overdueTaskReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/events/" + aliceEventId + "/tasks")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nextWeekTaskReq)))
                .andExpect(status().isCreated());

        // Query tasks - should be ordered chronologically by due date
        mockMvc.perform(get("/api/events/" + aliceEventId + "/tasks")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdueTasks").value(1))
                .andExpect(jsonPath("$.tasks[0].title").value("Sign venue deposit contract"))
                .andExpect(jsonPath("$.tasks[0].isOverdue").value(true))
                .andExpect(jsonPath("$.tasks[1].title").value("Finalize playlist"))
                .andExpect(jsonPath("$.tasks[1].isOverdue").value(false))
                .andExpect(jsonPath("$.tasks[2].title").value("RSVP headcount lock"))
                .andExpect(jsonPath("$.tasks[2].isOverdue").value(false));
    }

    @Test
    void aiRateLimiterBlocksRapidFloodingSafely() throws Exception {
        // Make 15 requests to hit limit
        for (int i = 0; i < 15; i++) {
            rateLimiterService.checkLimit("test-burst-user");
        }

        // 16th request must throw RateLimitExceededException
        com.gatherly.common.RateLimitExceededException thrown = assertThrows(
                com.gatherly.common.RateLimitExceededException.class,
                () -> rateLimiterService.checkLimit("test-burst-user")
        );

        assertTrue(thrown.getMessage().contains("Too many AI planning requests"));
        rateLimiterService.clear("test-burst-user");
    }
}
