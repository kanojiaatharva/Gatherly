package com.gatherly.events;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ImpactAnalysisResponse(
    Long eventId,
    String currency,
    BudgetImpact budgetImpact,
    TimelineImpact timelineImpact,
    List<String> recommendations
) {
    public record BudgetImpact(
        BigDecimal originalBudget,
        BigDecimal proposedBudget,
        BigDecimal budgetDelta,
        BigDecimal currentEstimatedTotal,
        BigDecimal currentActualTotal,
        BigDecimal projectedRemaining,
        boolean isProjectedOverBudget,
        BigDecimal perGuestOriginal,
        BigDecimal perGuestProposed,
        List<CategoryCostImpact> categoryImpacts
    ) {}

    public record CategoryCostImpact(
        String category,
        BigDecimal originalEstimate,
        BigDecimal projectedEstimate,
        BigDecimal delta,
        String explanation
    ) {}

    public record TimelineImpact(
        Long originalDaysRemaining,
        Long proposedDaysRemaining,
        Long daysShifted,
        boolean isScheduleCompressed,
        int affectedTasksCount,
        int overdueTasksAfterShiftCount,
        List<TaskTimelineWarning> taskWarnings
    ) {}

    public record TaskTimelineWarning(
        Long taskId,
        String taskTitle,
        LocalDate currentDueDate,
        String warningReason,
        LocalDate suggestedNewDueDate
    ) {}
}
