package com.gatherly.ai;

import java.time.Instant;

public record PlanRevisionResponse(
    Long id,
    Long eventId,
    Integer versionNumber,
    String changeSummary,
    Instant createdAt
) {
    public static PlanRevisionResponse fromEntity(PlanRevision revision) {
        return new PlanRevisionResponse(
            revision.getId(),
            revision.getEvent().getId(),
            revision.getVersionNumber(),
            revision.getChangeSummary(),
            revision.getCreatedAt()
        );
    }
}
