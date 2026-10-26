export type EventType = 'Wedding' | 'Birthday' | 'Community event' | 'Anniversary' | 'Gala' | 'Other';

export interface User {
  id: number;
  email: string;
  displayName: string;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface EventItem {
  id: number;
  userId: number;
  name: string;
  eventType: string;
  eventDate: string | null;
  location: string | null;
  guestCount: number | null;
  currency: string;
  totalBudget: number;
  stylePreferences: string | null;
  dietaryRequirements: string | null;
  accessibilityNeeds: string | null;
  priorities: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface EventSummary {
  id: number;
  name: string;
  eventType: string;
  eventDate: string | null;
  location: string | null;
  guestCount: number | null;
  currency: string;
  totalBudget: number;
  createdAt: string;
}

export interface BudgetItem {
  id: number;
  eventId: number;
  category: string;
  description: string | null;
  estimatedAmount: number;
  actualAmount: number | null;
  isEstimate: boolean;
  createdAt: string;
}

export interface BudgetCategorySummary {
  category: string;
  estimatedTotal: number;
  actualTotal: number;
  itemCount: number;
}

export interface BudgetOverview {
  eventId: number;
  currency: string;
  totalBudget: number;
  totalEstimated: number;
  totalActual: number;
  remainingBudget: number;
  isOverBudget: boolean;
  variance: number;
  totalItemsCount: number;
  estimateCount: number;
  confirmedCount: number;
  categories: BudgetCategorySummary[];
  items: BudgetItem[];
}

export interface TaskItem {
  id: number;
  eventId: number;
  title: string;
  description: string | null;
  dueDate: string | null;
  status: 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  assigneeLabel: string | null;
  isOverdue: boolean;
  daysUntilDue: number | null;
  suggestedAlternativeDate: string | null;
  dependsOnTaskId?: number | null;
  dependsOnTaskTitle?: string | null;
  isBlocked?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ImpactAnalysisRequest {
  guestCount?: number;
  eventDate?: string;
  totalBudget?: number;
}

export interface CategoryCostImpact {
  category: string;
  originalEstimate: number;
  projectedEstimate: number;
  delta: number;
  explanation: string;
}

export interface TaskTimelineWarning {
  taskId: number;
  taskTitle: string;
  currentDueDate: string | null;
  warningReason: string;
  suggestedNewDueDate: string | null;
}

export interface ImpactAnalysisResponse {
  eventId: number;
  currency: string;
  budgetImpact: {
    originalBudget: number;
    proposedBudget: number;
    budgetDelta: number;
    currentEstimatedTotal: number;
    currentActualTotal: number;
    projectedRemaining: number;
    isProjectedOverBudget: boolean;
    perGuestOriginal: number;
    perGuestProposed: number;
    categoryImpacts: CategoryCostImpact[];
  };
  timelineImpact: {
    originalDaysRemaining: number | null;
    proposedDaysRemaining: number | null;
    daysShifted: number;
    isScheduleCompressed: boolean;
    affectedTasksCount: number;
    overdueTasksAfterShiftCount: number;
    taskWarnings: TaskTimelineWarning[];
  };
  recommendations: string[];
}

export interface TaskOverview {
  eventId: number;
  totalTasks: number;
  completedTasks: number;
  inProgressTasks: number;
  todoTasks: number;
  overdueTasks: number;
  completionPercentage: number;
  tasks: TaskItem[];
}

export interface VendorItem {
  id: number;
  eventId: number;
  category: string;
  name: string | null;
  contactDetails: string | null;
  quotedAmount: number | null;
  quoteStatus: 'UNVERIFIED' | 'ESTIMATED' | 'QUOTED' | 'REJECTED';
  bookingStatus: 'NOT_BOOKED' | 'CONTACTED' | 'SHORTLISTED' | 'BOOKED' | 'CANCELLED';
  notes: string | null;
  source?: string | null;
  lastVerifiedAt?: string | null;
  createdAt: string;
}

export interface GeneratedBudgetItem {
  category: string;
  description: string;
  estimatedAmount: number;
}

export interface GeneratedTask {
  title: string;
  description: string;
  dueDate: string | null;
  priority: string;
  assigneeLabel: string;
}

export interface GeneratedIdea {
  category: string;
  title: string;
  description: string;
}

export interface GeneratedVendorCategory {
  category: string;
  description: string;
  tipsForVetting: string[];
}

export interface GeneratedTimelinePhase {
  phaseName: string;
  timeframe: string;
  keyMilestones: string[];
}

export interface GeneratedPlan {
  summary: string;
  assumptions?: string[];
  themeSuggestions: string[];
  budgetItems: GeneratedBudgetItem[];
  tasks: GeneratedTask[];
  timeline?: GeneratedTimelinePhase[];
  ideas: GeneratedIdea[];
  recommendedVendorCategories: GeneratedVendorCategory[];
  budgetWarning: string | null;
  tradeOffSuggestions?: string[];
  totalCalculatedBudget: number;
  isOverBudget: boolean;
  followUpQuestions?: string[];
  providerMode?: string;
  providerNotice?: string | null;
}

export interface ProposedBudgetItemDiff {
  action: 'ADD' | 'UPDATE' | 'REMOVE';
  category: string;
  description: string;
  oldAmount: number;
  newAmount: number;
  reason: string;
}

export interface ProposedTaskDiff {
  action: 'ADD' | 'UPDATE' | 'REMOVE';
  title: string;
  description: string;
  dueDate: string | null;
  priority: string;
  reason: string;
}

export interface PlanRevisionProposal {
  eventId: number;
  prompt: string;
  changeSummary: string;
  budgetAdjustments: ProposedBudgetItemDiff[];
  taskAdjustments: ProposedTaskDiff[];
  newVendorCategories: string[];
  newIdeas: GeneratedIdea[];
  proposedTotalEstimated: number;
  isOverBudget: boolean;
}

export interface PlanRevision {
  id: number;
  eventId: number;
  versionNumber: number;
  changeSummary: string;
  createdAt: string;
}
