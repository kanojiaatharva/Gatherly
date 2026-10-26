package com.gatherly.ai;

import jakarta.validation.constraints.NotNull;

public record ApplyRevisionRequest(
    @NotNull(message = "Plan revision proposal is required")
    PlanRevisionProposal proposal
) {}
