package com.gatherly.ai;

import java.math.BigDecimal;

public record GeneratedBudgetItem(
    String category,
    String description,
    BigDecimal estimatedAmount
) {}
