package com.gatherly.tasks;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateTaskRequest(
    @NotBlank(message = "Task title is required")
    @Size(max = 220, message = "Task title cannot exceed 220 characters")
    String title,

    String description,

    LocalDate dueDate,

    @Pattern(regexp = "^(?i)(TODO|IN_PROGRESS|COMPLETED|CANCELLED)$", message = "Status must be TODO, IN_PROGRESS, COMPLETED, or CANCELLED")
    String status,

    @Pattern(regexp = "^(?i)(LOW|MEDIUM|HIGH|URGENT)$", message = "Priority must be LOW, MEDIUM, HIGH, or URGENT")
    String priority,

    @Size(max = 120, message = "Assignee label cannot exceed 120 characters")
    String assigneeLabel,

    Long dependsOnTaskId
) {
    public CreateTaskRequest(String title, String description, LocalDate dueDate, String status, String priority, String assigneeLabel) {
        this(title, description, dueDate, status, priority, assigneeLabel, null);
    }
}
