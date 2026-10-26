package com.gatherly.budget;

import java.math.BigDecimal;
import java.util.List;

public record BudgetOverviewResponse(
    Long eventId,
    String currency,
    BigDecimal totalBudget,
    BigDecimal totalEstimated,
    BigDecimal totalActual,
    BigDecimal remainingBudget,
    boolean isOverBudget,
    BigDecimal variance,
    int totalItemsCount,
    int estimateCount,
    int confirmedCount,
    List<BudgetCategorySummary> categories,
    List<BudgetItemResponse> items
) {}
