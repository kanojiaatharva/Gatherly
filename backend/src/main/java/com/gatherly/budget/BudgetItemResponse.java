package com.gatherly.budget;

import java.math.BigDecimal;
import java.time.Instant;

public record BudgetItemResponse(
    Long id,
    Long eventId,
    String category,
    String description,
    BigDecimal estimatedAmount,
    BigDecimal actualAmount,
    Boolean isEstimate,
    Instant createdAt
) {
    public static BudgetItemResponse fromEntity(BudgetItem item) {
        return new BudgetItemResponse(
            item.getId(),
            item.getEvent().getId(),
            item.getCategory(),
            item.getDescription(),
            item.getEstimatedAmount(),
            item.getActualAmount(),
            item.getIsEstimate(),
            item.getCreatedAt()
        );
    }
}
