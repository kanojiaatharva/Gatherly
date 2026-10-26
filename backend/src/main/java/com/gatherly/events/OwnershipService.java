package com.gatherly.events;

import com.gatherly.common.ResourceNotFoundException;
import com.gatherly.common.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OwnershipService {

    private final EventRepository eventRepository;

    public OwnershipService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public Event verifyAndGetEvent(Long eventId, Long userId) {
        if (eventId == null) {
            throw new ResourceNotFoundException("Event ID is required");
        }
        if (userId == null) {
            throw new UnauthorizedException("User authentication required");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        if (!event.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied: You do not have permission to access this event");
        }

        return event;
    }
}
