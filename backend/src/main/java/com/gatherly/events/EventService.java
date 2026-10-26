package com.gatherly.events;

import com.gatherly.auth.User;
import com.gatherly.auth.UserRepository;
import com.gatherly.common.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final OwnershipService ownershipService;

    public EventService(
            EventRepository eventRepository,
            UserRepository userRepository,
            OwnershipService ownershipService) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.ownershipService = ownershipService;
    }

    @Transactional(readOnly = true)
    public List<EventSummaryResponse> getUserEvents(Long userId) {
        return eventRepository.findByUserIdOrderByEventDateAscCreatedAtDesc(userId)
                .stream()
                .map(EventSummaryResponse::fromEntity)
                .toList();
    }

    @Transactional
    public EventResponse createEvent(Long userId, CreateEventRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Event event = new Event(
                user,
                request.name().trim(),
                request.eventType().trim(),
                request.eventDate(),
                request.location() != null ? request.location().trim() : null,
                request.guestCount(),
                request.currency() != null ? request.currency().toUpperCase().trim() : "INR",
                request.totalBudget() != null ? request.totalBudget() : BigDecimal.ZERO,
                request.stylePreferences(),
                request.dietaryRequirements(),
                request.accessibilityNeeds(),
                request.priorities()
        );

        Event saved = eventRepository.save(event);
        return EventResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(Long eventId, Long userId) {
        Event event = ownershipService.verifyAndGetEvent(eventId, userId);
        return EventResponse.fromEntity(event);
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, Long userId, UpdateEventRequest request) {
        Event event = ownershipService.verifyAndGetEvent(eventId, userId);

        if (request.name() != null && !request.name().trim().isEmpty()) {
            event.setName(request.name().trim());
        }
        if (request.eventType() != null && !request.eventType().trim().isEmpty()) {
            event.setEventType(request.eventType().trim());
        }
        if (request.eventDate() != null) {
            event.setEventDate(request.eventDate());
        }
        if (request.location() != null) {
            event.setLocation(request.location().trim());
        }
        if (request.guestCount() != null) {
            event.setGuestCount(request.guestCount());
        }
        if (request.currency() != null && !request.currency().trim().isEmpty()) {
            event.setCurrency(request.currency().toUpperCase().trim());
        }
        if (request.totalBudget() != null) {
            event.setTotalBudget(request.totalBudget());
        }
        if (request.stylePreferences() != null) {
            event.setStylePreferences(request.stylePreferences());
        }
        if (request.dietaryRequirements() != null) {
            event.setDietaryRequirements(request.dietaryRequirements());
        }
        if (request.accessibilityNeeds() != null) {
            event.setAccessibilityNeeds(request.accessibilityNeeds());
        }
        if (request.priorities() != null) {
            event.setPriorities(request.priorities());
        }

        Event saved = eventRepository.save(event);
        return EventResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteEvent(Long eventId, Long userId) {
        Event event = ownershipService.verifyAndGetEvent(eventId, userId);
        eventRepository.delete(event);
    }
}
