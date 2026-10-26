package com.gatherly.ai;

import java.time.LocalDate;

public record GeneratedTask(
    String title,
    String description,
    LocalDate dueDate,
    String priority,
    String assigneeLabel
) {}
