package com.gatherly.events;

import com.gatherly.budget.BudgetItem;
import com.gatherly.budget.BudgetItemRepository;
import com.gatherly.tasks.Task;
import com.gatherly.tasks.TaskRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImpactAnalysisService {

    private final OwnershipService ownershipService;
    private final BudgetItemRepository budgetItemRepository;
    private final TaskRepository taskRepository;

    private static final Set<String> GUEST_SENSITIVE_KEYWORDS = Set.of(
            "cater", "food", "dinner", "lunch", "buffet", "menu", "bar", "beverage",
            "cocktail", "drink", "table", "chair", "rental", "favor", "invitation", "stationery"
    );

    public ImpactAnalysisService(
            OwnershipService ownershipService,
            BudgetItemRepository budgetItemRepository,
            TaskRepository taskRepository) {
        this.ownershipService = ownershipService;
        this.budgetItemRepository = budgetItemRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public ImpactAnalysisResponse analyzeImpact(Long eventId, Long userId, ImpactAnalysisRequest request) {
        Event event = ownershipService.verifyAndGetEvent(eventId, userId);
        List<BudgetItem> currentBudgetItems = budgetItemRepository.findByEventIdOrderByCreatedAtAsc(eventId);
        List<Task> currentTasks = taskRepository.findByEventIdOrderByDueDateAscCreatedAtAsc(eventId);

        // 1. Budget Impact Calculations
        BigDecimal originalBudget = event.getTotalBudget() != null
                ? event.getTotalBudget().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal proposedBudget = request.totalBudget() != null
                ? request.totalBudget().setScale(2, RoundingMode.HALF_UP)
                : originalBudget;

        BigDecimal budgetDelta = proposedBudget.subtract(originalBudget);

        BigDecimal currentEstimatedTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentActualTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (BudgetItem item : currentBudgetItems) {
            if (item.getEstimatedAmount() != null) {
                currentEstimatedTotal = currentEstimatedTotal.add(item.getEstimatedAmount().setScale(2, RoundingMode.HALF_UP));
            }
            if (item.getActualAmount() != null) {
                currentActualTotal = currentActualTotal.add(item.getActualAmount().setScale(2, RoundingMode.HALF_UP));
            }
        }

        BigDecimal projectedRemaining = proposedBudget.subtract(currentEstimatedTotal);
        boolean isProjectedOverBudget = currentEstimatedTotal.compareTo(proposedBudget) > 0
                || currentActualTotal.compareTo(proposedBudget) > 0;

        int originalGuests = event.getGuestCount() != null && event.getGuestCount() > 0 ? event.getGuestCount() : 1;
        int proposedGuests = request.guestCount() != null && request.guestCount() > 0 ? request.guestCount() : originalGuests;

        BigDecimal perGuestOriginal = originalBudget.divide(BigDecimal.valueOf(originalGuests), 2, RoundingMode.HALF_UP);
        BigDecimal perGuestProposed = proposedBudget.divide(BigDecimal.valueOf(proposedGuests), 2, RoundingMode.HALF_UP);

        // Analyze category cost impacts based on guest count changes
        List<ImpactAnalysisResponse.CategoryCostImpact> categoryImpacts = new ArrayList<>();
        double guestScaleRatio = (double) proposedGuests / (double) originalGuests;

        if (originalGuests != proposedGuests && !currentBudgetItems.isEmpty()) {
            Map<String, BigDecimal> categoryEstimates = new LinkedHashMap<>();
            for (BudgetItem item : currentBudgetItems) {
                if (item.getEstimatedAmount() != null) {
                    categoryEstimates.merge(item.getCategory(), item.getEstimatedAmount(), BigDecimal::add);
                }
            }

            for (Map.Entry<String, BigDecimal> entry : categoryEstimates.entrySet()) {
                String catLower = entry.getKey().toLowerCase();
                boolean isSensitive = GUEST_SENSITIVE_KEYWORDS.stream().anyMatch(catLower::contains);
                if (isSensitive) {
                    BigDecimal origEst = entry.getValue().setScale(2, RoundingMode.HALF_UP);
                    BigDecimal projEst = origEst.multiply(BigDecimal.valueOf(guestScaleRatio)).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal catDelta = projEst.subtract(origEst);
                    String explanation = String.format("Variable headcount impact: scaling by %+.1f%% for %d guests",
                            (guestScaleRatio - 1.0) * 100.0, proposedGuests);
                    categoryImpacts.add(new ImpactAnalysisResponse.CategoryCostImpact(
                            entry.getKey(), origEst, projEst, catDelta, explanation));
                }
            }
        }

        // 2. Timeline Impact Calculations
        LocalDate today = LocalDate.now();
        Long originalDaysRemaining = event.getEventDate() != null
                ? ChronoUnit.DAYS.between(today, event.getEventDate())
                : null;

        LocalDate targetDate = request.eventDate() != null ? request.eventDate() : event.getEventDate();
        Long proposedDaysRemaining = targetDate != null
                ? ChronoUnit.DAYS.between(today, targetDate)
                : originalDaysRemaining;

        Long daysShifted = 0L;
        boolean isCompressed = false;
        if (originalDaysRemaining != null && proposedDaysRemaining != null) {
            daysShifted = proposedDaysRemaining - originalDaysRemaining;
            isCompressed = daysShifted < 0;
        }

        List<ImpactAnalysisResponse.TaskTimelineWarning> taskWarnings = new ArrayList<>();
        int overdueAfterShiftCount = 0;

        for (Task t : currentTasks) {
            if ("COMPLETED".equalsIgnoreCase(t.getStatus()) || "CANCELLED".equalsIgnoreCase(t.getStatus())) {
                continue;
            }

            if (t.getDueDate() != null && targetDate != null) {
                // Warning 1: task due date falls after new event date
                if (t.getDueDate().isAfter(targetDate)) {
                    LocalDate suggested = targetDate.minusDays(3).isAfter(today) ? targetDate.minusDays(3) : today.plusDays(1);
                    taskWarnings.add(new ImpactAnalysisResponse.TaskTimelineWarning(
                            t.getId(),
                            t.getTitle(),
                            t.getDueDate(),
                            "Due date is after the newly proposed celebration date (" + targetDate + ")",
                            suggested
                    ));
                    overdueAfterShiftCount++;
                } else if (daysShifted < 0) {
                    // Schedule compressed: check if task due date would be in the past or dangerously compressed
                    LocalDate shiftedTaskDueDate = t.getDueDate().plusDays(daysShifted);
                    if (shiftedTaskDueDate.isBefore(today)) {
                        taskWarnings.add(new ImpactAnalysisResponse.TaskTimelineWarning(
                                t.getId(),
                                t.getTitle(),
                                t.getDueDate(),
                                "Compressed schedule would push this task deadline into the past (" + shiftedTaskDueDate + ")",
                                today.plusDays(2).isBefore(targetDate) ? today.plusDays(2) : targetDate.minusDays(1)
                        ));
                        overdueAfterShiftCount++;
                    }
                }
            }
        }

        // 3. Actionable Recommendations
        List<String> recommendations = new ArrayList<>();
        if (proposedGuests > originalGuests) {
            int extraGuests = proposedGuests - originalGuests;
            recommendations.add(String.format("Guest headcount increased by +%d: Expect per-person food, beverage, and tableware costs to increase. Verify venue capacity fire limits.", extraGuests));
        } else if (proposedGuests < originalGuests) {
            int reducedGuests = originalGuests - proposedGuests;
            recommendations.add(String.format("Headcount reduced by -%d: Potential savings across catering and favors. You can reallocate funds to upgrade entertainment or photography.", reducedGuests));
        }

        if (isProjectedOverBudget) {
            BigDecimal overage = currentEstimatedTotal.subtract(proposedBudget).max(BigDecimal.ZERO);
            recommendations.add(String.format("Budget alert: Proposed budget (%s %s) is below current estimated expenses (%s %s) by %s %s. Consider trimming non-essential items.",
                    event.getCurrency(), proposedBudget, event.getCurrency(), currentEstimatedTotal, event.getCurrency(), overage));
        } else if (budgetDelta.compareTo(BigDecimal.ZERO) > 0) {
            recommendations.add(String.format("Budget expanded by +%s %s: You now have %s %s in unallocated headroom.",
                    event.getCurrency(), budgetDelta, event.getCurrency(), projectedRemaining));
        }

        if (isCompressed) {
            recommendations.add(String.format("Planning schedule accelerated by %d days: %d tasks are impacted. Prioritize venue, catering, and invitations immediately.",
                    Math.abs(daysShifted), taskWarnings.size()));
        } else if (daysShifted > 0) {
            recommendations.add(String.format("Event postponed by +%d days: Added breathing room for vendor inquiries and guest RSVPs. Verify vendor deposit hold policies.",
                    daysShifted));
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Current plan structure fits comfortably within proposed parameters. No urgent trade-offs required.");
        }

        ImpactAnalysisResponse.BudgetImpact budgetImpact = new ImpactAnalysisResponse.BudgetImpact(
                originalBudget,
                proposedBudget,
                budgetDelta,
                currentEstimatedTotal,
                currentActualTotal,
                projectedRemaining,
                isProjectedOverBudget,
                perGuestOriginal,
                perGuestProposed,
                categoryImpacts
        );

        ImpactAnalysisResponse.TimelineImpact timelineImpact = new ImpactAnalysisResponse.TimelineImpact(
                originalDaysRemaining,
                proposedDaysRemaining,
                daysShifted,
                isCompressed,
                taskWarnings.size(),
                overdueAfterShiftCount,
                taskWarnings
        );

        return new ImpactAnalysisResponse(
                event.getId(),
                event.getCurrency(),
                budgetImpact,
                timelineImpact,
                recommendations
        );
    }
}
