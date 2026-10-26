package com.gatherly.ai;

import java.math.BigDecimal;
import java.util.List;

public record GeneratedPlanResponse(
    String summary,
    List<String> assumptions,
    List<String> themeSuggestions,
    List<GeneratedBudgetItem> budgetItems,
    List<GeneratedTask> tasks,
    List<GeneratedTimelinePhase> timeline,
    List<GeneratedIdea> ideas,
    List<GeneratedVendorCategory> recommendedVendorCategories,
    String budgetWarning,
    List<String> tradeOffSuggestions,
    BigDecimal totalCalculatedBudget,
    boolean isOverBudget,
    List<String> followUpQuestions,
    String providerMode,
    String providerNotice
) {}
