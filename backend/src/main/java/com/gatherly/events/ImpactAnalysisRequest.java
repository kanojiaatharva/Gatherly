package com.gatherly.events;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ImpactAnalysisRequest(
    @Min(value = 1, message = "Guest count must be at least 1")
    Integer guestCount,

    LocalDate eventDate,

    @DecimalMin(value = "0.00", message = "Budget cannot be negative")
    BigDecimal totalBudget
) {}
