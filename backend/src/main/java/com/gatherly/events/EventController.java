package com.gatherly.events;

import com.gatherly.auth.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final ImpactAnalysisService impactAnalysisService;

    public EventController(EventService eventService, ImpactAnalysisService impactAnalysisService) {
        this.eventService = eventService;
        this.impactAnalysisService = impactAnalysisService;
    }

    @PostMapping("/{eventId}/impact-analysis")
    public ResponseEntity<ImpactAnalysisResponse> analyzeImpact(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long eventId,
            @Valid @RequestBody ImpactAnalysisRequest request) {
        ImpactAnalysisResponse response = impactAnalysisService.analyzeImpact(eventId, principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<EventSummaryResponse>> getEvents(@AuthenticationPrincipal UserPrincipal principal) {
        List<EventSummaryResponse> events = eventService.getUserEvents(principal.getId());
        return ResponseEntity.ok(events);
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateEventRequest request) {
        EventResponse response = eventService.createEvent(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEvent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long eventId) {
        EventResponse response = eventService.getEvent(eventId, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventResponse> updateEvent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventRequest request) {
        EventResponse response = eventService.updateEvent(eventId, principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long eventId) {
        eventService.deleteEvent(eventId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
