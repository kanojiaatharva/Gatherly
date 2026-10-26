package com.gatherly.ai;

import java.time.LocalDate;

public record ProposedTaskDiff(
    String action, // ADD, UPDATE, REMOVE
    String title,
    String description,
    LocalDate dueDate,
    String priority,
    String reason
) {}
