package com.gatherly.ai;

import java.math.BigDecimal;

public record ProposedBudgetItemDiff(
    String action, // ADD, UPDATE, REMOVE
    String category,
    String description,
    BigDecimal oldAmount,
    BigDecimal newAmount,
    String reason
) {}
