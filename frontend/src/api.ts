import {
  AuthResponse,
  User,
  EventItem,
  EventSummary,
  BudgetOverview,
  BudgetItem,
  TaskOverview,
  TaskItem,
  VendorItem,
  GeneratedPlan,
  PlanRevisionProposal,
  PlanRevision,
  ImpactAnalysisRequest,
  ImpactAnalysisResponse
} from './types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

class ApiClient {
  private token: string | null = null;

  constructor() {
    if (typeof localStorage !== 'undefined') {
      this.token = localStorage.getItem('gatherly_token');
    }
  }

  setToken(token: string | null) {
    this.token = token;
    if (typeof localStorage !== 'undefined') {
      if (token) {
        localStorage.setItem('gatherly_token', token);
      } else {
        localStorage.removeItem('gatherly_token');
      }
    }
  }

  getToken(): string | null {
    return this.token;
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string>),
    };

    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    }

    const response = await fetch(`${BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    if (response.status === 204) {
      return {} as T;
    }

    const data = await response.json().catch(() => null);

    if (!response.ok) {
      const errorMessage =
        data?.message ||
        (data?.fieldErrors ? Object.values(data.fieldErrors).join(', ') : null) ||
        data?.error ||
        `Request failed with status ${response.status}`;
      throw new Error(errorMessage);
    }

    return data as T;
  }

  // Auth
  async register(email: string, password: string, displayName?: string): Promise<AuthResponse> {
    const res = await this.request<AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, password, displayName }),
    });
    this.setToken(res.token);
    return res;
  }

  async login(email: string, password: string): Promise<AuthResponse> {
    const res = await this.request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    this.setToken(res.token);
    return res;
  }

  async getMe(): Promise<User> {
    return this.request<User>('/auth/me');
  }

  logout() {
    this.setToken(null);
  }

  // Events
  async listEvents(): Promise<EventSummary[]> {
    return this.request<EventSummary[]>('/events');
  }

  async createEvent(event: Partial<EventItem>): Promise<EventItem> {
    return this.request<EventItem>('/events', {
      method: 'POST',
      body: JSON.stringify(event),
    });
  }

  async getEvent(id: number): Promise<EventItem> {
    return this.request<EventItem>(`/events/${id}`);
  }

  async updateEvent(id: number, updates: Partial<EventItem>): Promise<EventItem> {
    return this.request<EventItem>(`/events/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(updates),
    });
  }

  async deleteEvent(id: number): Promise<void> {
    await this.request<void>(`/events/${id}`, {
      method: 'DELETE',
    });
  }

  async analyzeImpact(eventId: number, request: ImpactAnalysisRequest): Promise<ImpactAnalysisResponse> {
    return this.request<ImpactAnalysisResponse>(`/events/${eventId}/impact-analysis`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  // Budget
  async getBudget(eventId: number): Promise<BudgetOverview> {
    return this.request<BudgetOverview>(`/events/${eventId}/budget`);
  }

  async addBudgetItem(eventId: number, item: Partial<BudgetItem>): Promise<BudgetItem> {
    return this.request<BudgetItem>(`/events/${eventId}/budget-items`, {
      method: 'POST',
      body: JSON.stringify(item),
    });
  }

  async updateBudgetItem(eventId: number, itemId: number, updates: Partial<BudgetItem>): Promise<BudgetItem> {
    return this.request<BudgetItem>(`/events/${eventId}/budget-items/${itemId}`, {
      method: 'PATCH',
      body: JSON.stringify(updates),
    });
  }

  async deleteBudgetItem(eventId: number, itemId: number): Promise<void> {
    await this.request<void>(`/events/${eventId}/budget-items/${itemId}`, {
      method: 'DELETE',
    });
  }

  // Tasks
  async getTasks(eventId: number): Promise<TaskOverview> {
    return this.request<TaskOverview>(`/events/${eventId}/tasks`);
  }

  async createTask(eventId: number, task: Partial<TaskItem>): Promise<TaskItem> {
    return this.request<TaskItem>(`/events/${eventId}/tasks`, {
      method: 'POST',
      body: JSON.stringify(task),
    });
  }

  async updateTask(eventId: number, taskId: number, updates: Partial<TaskItem>): Promise<TaskItem> {
    return this.request<TaskItem>(`/events/${eventId}/tasks/${taskId}`, {
      method: 'PATCH',
      body: JSON.stringify(updates),
    });
  }

  async deleteTask(eventId: number, taskId: number): Promise<void> {
    await this.request<void>(`/events/${eventId}/tasks/${taskId}`, {
      method: 'DELETE',
    });
  }

  // Vendors
  async getVendors(eventId: number): Promise<VendorItem[]> {
    return this.request<VendorItem[]>(`/events/${eventId}/vendors`);
  }

  async createVendor(eventId: number, vendor: Partial<VendorItem>): Promise<VendorItem> {
    return this.request<VendorItem>(`/events/${eventId}/vendors`, {
      method: 'POST',
      body: JSON.stringify(vendor),
    });
  }

  async updateVendor(eventId: number, vendorId: number, updates: Partial<VendorItem>): Promise<VendorItem> {
    return this.request<VendorItem>(`/events/${eventId}/vendors/${vendorId}`, {
      method: 'PATCH',
      body: JSON.stringify(updates),
    });
  }

  async deleteVendor(eventId: number, vendorId: number): Promise<void> {
    await this.request<void>(`/events/${eventId}/vendors/${vendorId}`, {
      method: 'DELETE',
    });
  }

  async bookVendor(eventId: number, vendorId: number, budgetItemId?: number): Promise<VendorItem> {
    const url = budgetItemId
      ? `/events/${eventId}/vendors/${vendorId}/book?budgetItemId=${budgetItemId}`
      : `/events/${eventId}/vendors/${vendorId}/book`;
    return this.request<VendorItem>(url, {
      method: 'POST',
    });
  }

  async getVendorSearchAdapterStatus(eventId: number): Promise<{ adapterAvailable: boolean; adapterName: string; policy: string }> {
    return this.request<{ adapterAvailable: boolean; adapterName: string; policy: string }>(`/events/${eventId}/vendors/search-adapter-status`);
  }

  // AI Planning
  async generatePlan(eventId: number, prompt?: string): Promise<GeneratedPlan> {
    return this.request<GeneratedPlan>(`/events/${eventId}/plan/generate`, {
      method: 'POST',
      body: JSON.stringify({ prompt: prompt || '' }),
    });
  }

  async proposeRevision(eventId: number, prompt: string): Promise<PlanRevisionProposal> {
    return this.request<PlanRevisionProposal>(`/events/${eventId}/plan/revise`, {
      method: 'POST',
      body: JSON.stringify({ prompt }),
    });
  }

  async applyRevision(eventId: number, proposal: PlanRevisionProposal): Promise<PlanRevision> {
    return this.request<PlanRevision>(`/events/${eventId}/plan/revisions/apply`, {
      method: 'POST',
      body: JSON.stringify({ proposal }),
    });
  }

  async getRevisions(eventId: number): Promise<PlanRevision[]> {
    return this.request<PlanRevision[]>(`/events/${eventId}/revisions`);
  }
}

export const api = new ApiClient();
