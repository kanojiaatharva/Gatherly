package com.gatherly.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record EventSummaryResponse(
    Long id,
    String name,
    String eventType,
    LocalDate eventDate,
    String location,
    Integer guestCount,
    String currency,
    BigDecimal totalBudget,
    Instant createdAt
) {
    public static EventSummaryResponse fromEntity(Event event) {
        return new EventSummaryResponse(
            event.getId(),
            event.getName(),
            event.getEventType(),
            event.getEventDate(),
            event.getLocation(),
            event.getGuestCount(),
            event.getCurrency(),
            event.getTotalBudget(),
            event.getCreatedAt()
        );
    }
}
