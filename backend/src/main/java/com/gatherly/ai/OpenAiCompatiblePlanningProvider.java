package com.gatherly.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatherly.budget.BudgetItem;
import com.gatherly.common.AiProviderException;
import com.gatherly.events.Event;
import com.gatherly.tasks.Task;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class OpenAiCompatiblePlanningProvider implements AiPlanningProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatiblePlanningProvider.class);

    private final String provider;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final int timeoutSeconds;
    private final DeterministicRuleBasedPlanningProvider fallbackProvider;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatiblePlanningProvider(
            @Value("${ai.provider:mock}") String provider,
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.model:gpt-4o-mini}") String model,
            @Value("${ai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ai.timeout-seconds:25}") int timeoutSeconds,
            DeterministicRuleBasedPlanningProvider fallbackProvider,
            ObjectMapper objectMapper) {
        this.provider = provider != null ? provider.trim().toLowerCase() : "mock";
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null && !model.isBlank() ? model.trim() : "gpt-4o-mini";
        this.baseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl.trim() : "https://api.openai.com/v1";
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 25;
        this.fallbackProvider = fallbackProvider;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String getProviderMode() {
        if (isLiveConfigured()) {
            return "LIVE_MODEL";
        }
        return "DEVELOPMENT_MOCK";
    }

    private boolean isLiveConfigured() {
        if ("mock".equalsIgnoreCase(provider)) {
            return false;
        }
        return !apiKey.isEmpty() && !"replace_me".equalsIgnoreCase(apiKey) && !"test-key".equalsIgnoreCase(apiKey);
    }

    @Override
    public GeneratedPlanResponse generatePlan(Event event, String promptNotes) {
        if (!isLiveConfigured()) {
            log.info("AI planning running in DEVELOPMENT_MOCK mode (credentials absent or provider=mock).");
            return fallbackProvider.generatePlan(event, promptNotes);
        }

        String prompt = buildPlanPrompt(event, promptNotes);
        log.info("Dispatching plan generation request to live AI provider [model={}] for eventId={}", model, event.getId());

        String jsonOutput;
        try {
            jsonOutput = callLlm(prompt);
        } catch (AiProviderException ape) {
            throw ape;
        } catch (Exception e) {
            log.error("AI Provider communication error: {}", e.getMessage());
            throw new AiProviderException("AI provider communication failure: " + e.getMessage(), e);
        }

        // Validate JSON and retry once for recoverable malformed output
        try {
            return parseAndValidatePlan(jsonOutput, event);
        } catch (Exception initialParseEx) {
            log.warn("Model response failed schema parsing: {}. Retrying once with error correction prompt...", initialParseEx.getMessage());
            try {
                String retryPrompt = prompt + "\n\nCRITICAL FIX: Your previous response failed validation with error: "
                        + initialParseEx.getMessage() + ". You must output ONLY well-formed JSON matching the specified schema exactly.";
                String retryJson = callLlm(retryPrompt);
                return parseAndValidatePlan(retryJson, event);
            } catch (Exception retryEx) {
                log.error("Model response failed schema validation after retry: {}", retryEx.getMessage());
                throw new AiProviderException("AI model returned malformed output after retry: " + retryEx.getMessage(), retryEx);
            }
        }
    }

    @Override
    public PlanRevisionProposal proposeRevision(
            Event event,
            List<BudgetItem> currentBudget,
            List<Task> currentTasks,
            String userInstruction) {
        if (!isLiveConfigured()) {
            return fallbackProvider.proposeRevision(event, currentBudget, currentTasks, userInstruction);
        }

        String prompt;
        try {
            prompt = buildRevisionPrompt(event, currentBudget, currentTasks, userInstruction);
        } catch (Exception e) {
            throw new AiProviderException("Failed to serialize revision prompt context: " + e.getMessage(), e);
        }
        log.info("Dispatching revision request to live AI provider [model={}] for eventId={}", model, event.getId());

        String jsonOutput;
        try {
            jsonOutput = callLlm(prompt);
        } catch (AiProviderException ape) {
            throw ape;
        } catch (Exception e) {
            log.error("AI Provider revision communication error: {}", e.getMessage());
            throw new AiProviderException("AI provider communication failure during revision: " + e.getMessage(), e);
        }

        try {
            return parseAndValidateRevision(jsonOutput, event, currentBudget, userInstruction);
        } catch (Exception initialParseEx) {
            log.warn("Model revision failed schema parsing: {}. Retrying once with error correction prompt...", initialParseEx.getMessage());
            try {
                String retryPrompt = prompt + "\n\nCRITICAL FIX: Your previous revision response failed validation with error: "
                        + initialParseEx.getMessage() + ". You must output ONLY valid JSON matching the exact schema.";
                String retryJson = callLlm(retryPrompt);
                return parseAndValidateRevision(retryJson, event, currentBudget, userInstruction);
            } catch (Exception retryEx) {
                log.error("Model revision failed schema validation after retry: {}", retryEx.getMessage());
                throw new AiProviderException("AI model returned malformed revision output after retry: " + retryEx.getMessage(), retryEx);
            }
        }
    }

    private String callLlm(String systemAndUserPrompt) throws Exception {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are Gatherly, an expert event planning AI. You respond STRICTLY in JSON format without markdown code fences."),
                        Map.of("role", "user", "content", systemAndUserPrompt)
                ),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.6
        );

        String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new AiProviderException("AI provider authentication failed (HTTP " + response.statusCode() + "). Please check AI_API_KEY.");
        }
        if (response.statusCode() == 429) {
            throw new AiProviderException("AI provider rate limit reached (HTTP 429). Please try again in a few moments.");
        }
        if (response.statusCode() >= 500) {
            throw new AiProviderException("AI provider service unavailable (HTTP " + response.statusCode() + ").");
        }
        if (response.statusCode() != 200) {
            String sanitizedError = "AI provider returned HTTP " + response.statusCode();
            try {
                JsonNode errJson = objectMapper.readTree(response.body());
                if (errJson.has("error") && errJson.get("error").has("message")) {
                    sanitizedError += ": " + errJson.get("error").get("message").asText();
                }
            } catch (Exception ignored) {
            }
            throw new AiProviderException(sanitizedError);
        }

        JsonNode root = objectMapper.readTree(response.body());
        return root.path("choices").path(0).path("message").path("content").asText();
    }

    private String buildPlanPrompt(Event event, String promptNotes) {
        return """
            Generate a comprehensive, structured event plan for:
            Type: %s
            Name: %s
            Date: %s
            Location: %s
            Guests: %d
            Target Budget: %s %s
            Style Preferences: %s
            Dietary Requirements: %s
            Accessibility Needs: %s
            Priorities: %s
            User Notes: %s

            Return a valid JSON object matching this schema:
            {
              "summary": "string",
              "assumptions": ["string"],
              "themeSuggestions": ["string"],
              "budgetItems": [
                {"category": "string", "description": "string", "estimatedAmount": 0.00}
              ],
              "tasks": [
                {"title": "string", "description": "string", "dueDate": "YYYY-MM-DD", "priority": "LOW|MEDIUM|HIGH|URGENT", "assigneeLabel": "string"}
              ],
              "timeline": [
                {"phaseName": "string", "timeframe": "string", "keyMilestones": ["string"]}
              ],
              "ideas": [
                {"category": "string", "title": "string", "description": "string"}
              ],
              "recommendedVendorCategories": [
                {"category": "string", "description": "string", "tipsForVetting": ["string"]}
              ],
              "followUpQuestions": ["string"]
            }
            CRITICAL RULES:
            - Do NOT invent specific real-world company names, vendor phone numbers, or pretend bookings exist.
            - All budget items must be realistic estimates.
            - If essential details (date, guest count, location) are missing, state concise assumptions in "assumptions" and list concise questions in "followUpQuestions".
            """.formatted(
                event.getEventType(), event.getName(), event.getEventDate(), event.getLocation(),
                event.getGuestCount() != null ? event.getGuestCount() : 50,
                event.getTotalBudget(), event.getCurrency(),
                event.getStylePreferences(), event.getDietaryRequirements(),
                event.getAccessibilityNeeds(), event.getPriorities(),
                promptNotes != null ? promptNotes : ""
        );
    }

    private GeneratedPlanResponse parseAndValidatePlan(String json, Event event) throws Exception {
        JsonNode root = objectMapper.readTree(json);

        String summary = root.path("summary").asText("");
        if (summary.isBlank()) {
            throw new IllegalArgumentException("Missing required field 'summary'");
        }

        List<String> assumptions = new ArrayList<>();
        if (root.has("assumptions")) {
            for (JsonNode a : root.path("assumptions")) {
                assumptions.add(a.asText());
            }
        }

        List<String> themes = new ArrayList<>();
        if (root.has("themeSuggestions")) {
            for (JsonNode t : root.path("themeSuggestions")) {
                themes.add(t.asText());
            }
        }

        List<GeneratedBudgetItem> budgetItems = new ArrayList<>();
        BigDecimal totalCalculated = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (root.has("budgetItems") && root.path("budgetItems").isArray()) {
            for (JsonNode b : root.path("budgetItems")) {
                String cat = b.path("category").asText("Miscellaneous");
                String desc = b.path("description").asText("Estimated allocation");
                if (!desc.toLowerCase().contains("estimate")) {
                    desc = desc + " (Estimate)";
                }
                BigDecimal amt = BigDecimal.valueOf(b.path("estimatedAmount").asDouble(0.0))
                        .setScale(2, RoundingMode.HALF_UP);
                if (amt.compareTo(BigDecimal.ZERO) < 0) {
                    amt = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                }
                budgetItems.add(new GeneratedBudgetItem(cat, desc, amt));
                totalCalculated = totalCalculated.add(amt);
            }
        }

        if (budgetItems.isEmpty()) {
            throw new IllegalArgumentException("Plan must contain at least one budgetItem");
        }

        List<GeneratedTask> tasks = new ArrayList<>();
        if (root.has("tasks") && root.path("tasks").isArray()) {
            for (JsonNode t : root.path("tasks")) {
                String title = t.path("title").asText("Event preparation task");
                String desc = t.path("description").asText("");
                LocalDate due = null;
                try {
                    due = LocalDate.parse(t.path("dueDate").asText());
                } catch (Exception ignored) {
                    due = event.getEventDate() != null ? event.getEventDate().minusDays(14) : LocalDate.now().plusDays(14);
                }
                String priority = t.path("priority").asText("MEDIUM").toUpperCase();
                String assignee = t.path("assigneeLabel").asText("Host");
                tasks.add(new GeneratedTask(title, desc, due, priority, assignee));
            }
        }

        List<GeneratedTimelinePhase> timeline = new ArrayList<>();
        if (root.has("timeline") && root.path("timeline").isArray()) {
            for (JsonNode phase : root.path("timeline")) {
                String name = phase.path("phaseName").asText("Phase");
                String tf = phase.path("timeframe").asText("");
                List<String> milestones = new ArrayList<>();
                if (phase.has("keyMilestones")) {
                    for (JsonNode m : phase.path("keyMilestones")) {
                        milestones.add(m.asText());
                    }
                }
                timeline.add(new GeneratedTimelinePhase(name, tf, milestones));
            }
        }

        List<GeneratedIdea> ideas = new ArrayList<>();
        if (root.has("ideas") && root.path("ideas").isArray()) {
            for (JsonNode i : root.path("ideas")) {
                ideas.add(new GeneratedIdea(
                        i.path("category").asText("Idea"),
                        i.path("title").asText("Concept"),
                        i.path("description").asText("")
                ));
            }
        }

        List<GeneratedVendorCategory> vendorCategories = new ArrayList<>();
        if (root.has("recommendedVendorCategories") && root.path("recommendedVendorCategories").isArray()) {
            for (JsonNode v : root.path("recommendedVendorCategories")) {
                List<String> tips = new ArrayList<>();
                if (v.has("tipsForVetting")) {
                    for (JsonNode tip : v.path("tipsForVetting")) {
                        tips.add(tip.asText());
                    }
                }
                vendorCategories.add(new GeneratedVendorCategory(
                        v.path("category").asText("Vendor Category"),
                        v.path("description").asText(""),
                        tips
                ));
            }
        }

        List<String> followUpQuestions = new ArrayList<>();
        if (root.has("followUpQuestions") && root.path("followUpQuestions").isArray()) {
            for (JsonNode q : root.path("followUpQuestions")) {
                followUpQuestions.add(q.asText());
            }
        }

        // Budget check & trade-off suggestions computed in backend
        BigDecimal eventBudget = event.getTotalBudget() != null ? event.getTotalBudget() : BigDecimal.ZERO;
        boolean over = eventBudget.compareTo(BigDecimal.ZERO) > 0 && totalCalculated.compareTo(eventBudget) > 0;
        String warning = null;
        List<String> tradeOffSuggestions = new ArrayList<>();

        if (over) {
            BigDecimal diff = totalCalculated.subtract(eventBudget).setScale(2, RoundingMode.HALF_UP);
            warning = "The proposed plan estimated allocations (" + totalCalculated + " " + event.getCurrency()
                    + ") exceed your target budget (" + eventBudget + " " + event.getCurrency()
                    + ") by " + diff + " " + event.getCurrency() + ". Your target budget was preserved.";
            tradeOffSuggestions.add("Opt for buffet or family-style dinner over individual plated courses.");
            tradeOffSuggestions.add("Focus floral decor on ceremony arch and head table rather than all perimeter stations.");
            tradeOffSuggestions.add("Hire a curated DJ or acoustic duo instead of a full multi-piece live band.");
        }

        return new GeneratedPlanResponse(
                summary,
                assumptions,
                themes,
                budgetItems,
                tasks,
                timeline,
                ideas,
                vendorCategories,
                warning,
                tradeOffSuggestions,
                totalCalculated,
                over,
                followUpQuestions,
                getProviderMode(),
                null
        );
    }

    private String buildRevisionPrompt(
            Event event,
            List<BudgetItem> currentBudget,
            List<Task> currentTasks,
            String instruction) throws Exception {
        return """
            The user wants to revise an existing event plan.
            Event: %s (%s), Budget: %s %s.
            Current Budget Items: %s
            Current Tasks: %s
            User Revision Instruction: "%s"

            Return a valid JSON object matching this schema:
            {
              "summary": "string explaining proposed changes",
              "budgetAdjustments": [
                {"action": "ADD|UPDATE|REMOVE", "category": "string", "newAmount": 0.00, "description": "string"}
              ],
              "taskAdjustments": [
                {"action": "ADD|UPDATE|REMOVE", "title": "string", "description": "string", "dueDate": "YYYY-MM-DD", "priority": "LOW|MEDIUM|HIGH|URGENT", "assigneeLabel": "string"}
              ],
              "newVendorCategories": ["string"],
              "newIdeas": [
                {"category": "string", "title": "string", "description": "string"}
              ]
            }
            """.formatted(
                event.getName(), event.getEventType(), event.getTotalBudget(), event.getCurrency(),
                objectMapper.writeValueAsString(currentBudget.stream().map(b -> Map.of("category", b.getCategory(), "amount", b.getEstimatedAmount())).toList()),
                objectMapper.writeValueAsString(currentTasks.stream().map(t -> Map.of("title", t.getTitle(), "due", t.getDueDate())).toList()),
                instruction
        );
    }

    private PlanRevisionProposal parseAndValidateRevision(
            String json,
            Event event,
            List<BudgetItem> currentBudget,
            String instruction) throws Exception {
        JsonNode root = objectMapper.readTree(json);

        String summary = root.path("summary").asText("Proposed plan revision based on: " + instruction);
        List<ProposedBudgetItemDiff> budgetDiffs = new ArrayList<>();
        if (root.has("budgetAdjustments")) {
            for (JsonNode b : root.path("budgetAdjustments")) {
                String cat = b.path("category").asText("General");
                String desc = b.path("description").asText("");
                BigDecimal newAmt = BigDecimal.valueOf(b.path("newAmount").asDouble(0.0)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal oldAmt = currentBudget.stream()
                        .filter(cur -> cur.getCategory().equalsIgnoreCase(cat))
                        .findFirst()
                        .map(BudgetItem::getEstimatedAmount)
                        .orElse(BigDecimal.ZERO);
                budgetDiffs.add(new ProposedBudgetItemDiff(
                        b.path("action").asText("UPDATE").toUpperCase(),
                        cat,
                        desc,
                        oldAmt,
                        newAmt,
                        "Model proposed adjustment"
                ));
            }
        }

        List<ProposedTaskDiff> taskDiffs = new ArrayList<>();
        if (root.has("taskAdjustments")) {
            for (JsonNode t : root.path("taskAdjustments")) {
                LocalDate due = null;
                try {
                    due = LocalDate.parse(t.path("dueDate").asText());
                } catch (Exception ignored) {
                    due = event.getEventDate() != null ? event.getEventDate().minusDays(10) : LocalDate.now().plusDays(10);
                }
                taskDiffs.add(new ProposedTaskDiff(
                        t.path("action").asText("ADD").toUpperCase(),
                        t.path("title").asText("New task"),
                        t.path("description").asText(""),
                        due,
                        t.path("priority").asText("MEDIUM").toUpperCase(),
                        "Model proposed task"
                ));
            }
        }

        List<String> newVendors = new ArrayList<>();
        if (root.has("newVendorCategories")) {
            for (JsonNode v : root.path("newVendorCategories")) {
                newVendors.add(v.asText());
            }
        }

        List<GeneratedIdea> newIdeas = new ArrayList<>();
        if (root.has("newIdeas")) {
            for (JsonNode i : root.path("newIdeas")) {
                newIdeas.add(new GeneratedIdea(
                        i.path("category").asText("Idea"),
                        i.path("title").asText("Concept"),
                        i.path("description").asText("")
                ));
            }
        }

        BigDecimal currentTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (BudgetItem b : currentBudget) {
            if (b.getEstimatedAmount() != null) {
                currentTotal = currentTotal.add(b.getEstimatedAmount());
            }
        }

        BigDecimal proposedTotal = currentTotal;
        for (ProposedBudgetItemDiff diff : budgetDiffs) {
            if ("ADD".equalsIgnoreCase(diff.action())) {
                proposedTotal = proposedTotal.add(diff.newAmount());
            } else if ("REMOVE".equalsIgnoreCase(diff.action())) {
                proposedTotal = proposedTotal.subtract(diff.newAmount() != null ? diff.newAmount() : BigDecimal.ZERO);
            }
        }

        boolean isOver = event.getTotalBudget() != null
                && event.getTotalBudget().compareTo(BigDecimal.ZERO) > 0
                && proposedTotal.compareTo(event.getTotalBudget()) > 0;

        return new PlanRevisionProposal(
                event.getId(),
                instruction,
                summary,
                budgetDiffs,
                taskDiffs,
                newVendors,
                newIdeas,
                proposedTotal,
                isOver
        );
    }
}
