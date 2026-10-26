package com.gatherly.ai;

import java.math.BigDecimal;
import java.util.List;

public record PlanRevisionProposal(
    Long eventId,
    String prompt,
    String changeSummary,
    List<ProposedBudgetItemDiff> budgetAdjustments,
    List<ProposedTaskDiff> taskAdjustments,
    List<String> newVendorCategories,
    List<GeneratedIdea> newIdeas,
    BigDecimal proposedTotalEstimated,
    boolean isOverBudget
) {}
