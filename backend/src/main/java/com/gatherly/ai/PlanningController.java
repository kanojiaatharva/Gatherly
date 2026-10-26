package com.gatherly.ai;

import com.gatherly.auth.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}")
public class PlanningController {

    private final AiPlannerService aiPlannerService;

    public PlanningController(AiPlannerService aiPlannerService) {
        this.aiPlannerService = aiPlannerService;
    }

    @PostMapping("/plan/generate")
    public ResponseEntity<GeneratedPlanResponse> generatePlan(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) Map<String, String> body) {
        String promptNotes = body != null ? body.get("prompt") : null;
        GeneratedPlanResponse response = aiPlannerService.generatePlan(eventId, principal.getId(), promptNotes);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/plan/revise")
    public ResponseEntity<PlanRevisionProposal> proposeRevision(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PlanRevisionRequest request) {
        PlanRevisionProposal proposal = aiPlannerService.proposeRevision(eventId, principal.getId(), request);
        return ResponseEntity.ok(proposal);
    }

    @PostMapping("/plan/revisions/apply")
    public ResponseEntity<PlanRevisionResponse> applyRevision(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ApplyRevisionRequest request) {
        PlanRevisionResponse response = aiPlannerService.applyRevision(eventId, principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/revisions")
    public ResponseEntity<List<PlanRevisionResponse>> getRevisions(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<PlanRevisionResponse> revisions = aiPlannerService.getRevisions(eventId, principal.getId());
        return ResponseEntity.ok(revisions);
    }
}
