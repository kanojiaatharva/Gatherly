package com.gatherly.ai;

import com.gatherly.budget.BudgetItem;
import com.gatherly.events.Event;
import com.gatherly.tasks.Task;
import java.util.List;

public interface AiPlanningProvider {
    GeneratedPlanResponse generatePlan(Event event, String promptNotes);
    PlanRevisionProposal proposeRevision(Event event, List<BudgetItem> currentBudget, List<Task> currentTasks, String userInstruction);
    String getProviderMode();
}
