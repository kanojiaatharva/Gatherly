package com.gatherly.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record EventResponse(
    Long id,
    Long userId,
    String name,
    String eventType,
    LocalDate eventDate,
    String location,
    Integer guestCount,
    String currency,
    BigDecimal totalBudget,
    String stylePreferences,
    String dietaryRequirements,
    String accessibilityNeeds,
    String priorities,
    Instant createdAt,
    Instant updatedAt
) {
    public static EventResponse fromEntity(Event event) {
        return new EventResponse(
            event.getId(),
            event.getUser().getId(),
            event.getName(),
            event.getEventType(),
            event.getEventDate(),
            event.getLocation(),
            event.getGuestCount(),
            event.getCurrency(),
            event.getTotalBudget(),
            event.getStylePreferences(),
            event.getDietaryRequirements(),
            event.getAccessibilityNeeds(),
            event.getPriorities(),
            event.getCreatedAt(),
            event.getUpdatedAt()
        );
    }
}
