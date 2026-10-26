package com.gatherly.tasks;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record TaskResponse(
    Long id,
    Long eventId,
    String title,
    String description,
    LocalDate dueDate,
    String status,
    String priority,
    String assigneeLabel,
    boolean isOverdue,
    Long daysUntilDue,
    LocalDate suggestedAlternativeDate,
    Long dependsOnTaskId,
    String dependsOnTaskTitle,
    boolean isBlocked,
    Instant createdAt,
    Instant updatedAt
) {
    public static TaskResponse fromEntity(Task task, LocalDate eventDate) {
        boolean overdue = false;
        Long days = null;
        LocalDate alternative = null;

        if (task.getDueDate() != null) {
            LocalDate today = LocalDate.now();
            days = ChronoUnit.DAYS.between(today, task.getDueDate());
            boolean isCompleted = "COMPLETED".equalsIgnoreCase(task.getStatus())
                    || "CANCELLED".equalsIgnoreCase(task.getStatus());
            if (task.getDueDate().isBefore(today) && !isCompleted) {
                overdue = true;
                // Suggest today, or if event is soon, suggest day before event
                if (eventDate != null && eventDate.isAfter(today)) {
                    alternative = today.plusDays(2).isBefore(eventDate) ? today.plusDays(2) : eventDate.minusDays(1);
                } else {
                    alternative = today.plusDays(1);
                }
            }
        }

        Long depId = null;
        String depTitle = null;
        boolean blocked = false;
        if (task.getDependsOnTask() != null) {
            depId = task.getDependsOnTask().getId();
            depTitle = task.getDependsOnTask().getTitle();
            blocked = !"COMPLETED".equalsIgnoreCase(task.getDependsOnTask().getStatus())
                    && !"CANCELLED".equalsIgnoreCase(task.getDependsOnTask().getStatus());
        }

        return new TaskResponse(
            task.getId(),
            task.getEvent().getId(),
            task.getTitle(),
            task.getDescription(),
            task.getDueDate(),
            task.getStatus(),
            task.getPriority(),
            task.getAssigneeLabel(),
            overdue,
            days,
            alternative,
            depId,
            depTitle,
            blocked,
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}
