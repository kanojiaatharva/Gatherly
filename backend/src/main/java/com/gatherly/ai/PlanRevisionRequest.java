package com.gatherly.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlanRevisionRequest(
    @NotBlank(message = "Revision instruction cannot be blank")
    @Size(max = 1000, message = "Revision instruction cannot exceed 1000 characters")
    String prompt
) {}
