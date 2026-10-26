package com.gatherly.events;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateEventRequest(
    @NotBlank(message = "Event name is required")
    @Size(max = 180, message = "Event name cannot exceed 180 characters")
    String name,

    @NotBlank(message = "Event type is required")
    @Size(max = 40, message = "Event type cannot exceed 40 characters")
    String eventType,

    LocalDate eventDate,

    @Size(max = 255, message = "Location cannot exceed 255 characters")
    String location,

    @Min(value = 1, message = "Guest count must be at least 1")
    Integer guestCount,

    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter currency code (e.g. INR, USD, EUR)")
    String currency,

    @DecimalMin(value = "0.00", message = "Budget cannot be negative")
    BigDecimal totalBudget,

    String stylePreferences,
    String dietaryRequirements,
    String accessibilityNeeds,
    String priorities
) {}
