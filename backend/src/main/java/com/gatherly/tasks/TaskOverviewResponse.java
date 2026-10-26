package com.gatherly.tasks;

import java.util.List;

public record TaskOverviewResponse(
    Long eventId,
    int totalTasks,
    int completedTasks,
    int inProgressTasks,
    int todoTasks,
    int overdueTasks,
    int completionPercentage,
    List<TaskResponse> tasks
) {}
