package com.gatherly.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateBudgetItemRequest(
    @NotBlank(message = "Category is required")
    @Size(max = 80, message = "Category cannot exceed 80 characters")
    String category,

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    String description,

    @DecimalMin(value = "0.00", message = "Estimated amount cannot be negative")
    BigDecimal estimatedAmount,

    @DecimalMin(value = "0.00", message = "Actual amount cannot be negative")
    BigDecimal actualAmount,

    Boolean isEstimate
) {}
