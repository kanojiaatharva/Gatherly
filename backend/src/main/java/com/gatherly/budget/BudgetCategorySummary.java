package com.gatherly.budget;

import java.math.BigDecimal;

public record BudgetCategorySummary(
    String category,
    BigDecimal estimatedTotal,
    BigDecimal actualTotal,
    int itemCount
) {}
