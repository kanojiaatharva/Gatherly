import { useState, useEffect, useCallback } from 'react';
import { api } from './api';
import {
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
} from './types';
import {
  CalendarHeart,
  Sparkles,
  Wallet,
  ListChecks,
  Store,
  Plus,
  Trash2,
  Edit2,
  Calendar,
  MapPin,
  Users,
  AlertTriangle,
  History,
  CheckCircle2,
  Clock,
  LogOut,
  LogIn,
  RefreshCw,
  Lightbulb,
  X,
  TrendingUp,
  Lock,
  Unlock,
  CalendarDays,
  LayoutList,
  DollarSign,
} from 'lucide-react';
import { AuthModal } from './components/AuthModal';
import { NewEventModal } from './components/NewEventModal';
import { ConfirmDialog } from './components/ConfirmDialog';
import { RevisionModal } from './components/RevisionModal';
import { AddBudgetItemModal } from './components/AddBudgetItemModal';
import { AddTaskModal } from './components/AddTaskModal';
import { AddVendorModal } from './components/AddVendorModal';
import { ImpactAnalysisModal } from './components/ImpactAnalysisModal';

type NavTab = 'overview' | 'plan' | 'revisions' | 'budget' | 'checklist' | 'vendors';

export default function App() {
  // Auth state
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [authLoading, setAuthLoading] = useState(true);

  // Events state
  const [events, setEvents] = useState<EventSummary[]>([]);
  const [selectedEventId, setSelectedEventId] = useState<number | null>(null);
  const [selectedEvent, setSelectedEvent] = useState<EventItem | null>(null);
  const [newEventModalOpen, setNewEventModalOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState<EventItem | null>(null);

  // Domain data state
  const [activeTab, setActiveTab] = useState<NavTab>('overview');
  const [budget, setBudget] = useState<BudgetOverview | null>(null);
  const [taskOverview, setTaskOverview] = useState<TaskOverview | null>(null);
  const [vendors, setVendors] = useState<VendorItem[]>([]);
  const [revisions, setRevisions] = useState<PlanRevision[]>([]);
  const [generatedPlan, setGeneratedPlan] = useState<GeneratedPlan | null>(null);

  // Loading & error states
  const [loadingEvent, setLoadingEvent] = useState(false);
  const [planGenerating, setPlanGenerating] = useState(false);
  const [planError, setPlanError] = useState<string | null>(null);
  const [revisionProposing, setRevisionProposing] = useState(false);
  const [planNotes, setPlanNotes] = useState('');
  const [revisionPrompt, setRevisionPrompt] = useState('');
  const [alertMessage, setAlertMessage] = useState<{ type: 'info' | 'error' | 'success'; text: string } | null>(null);

  // Modals state
  const [activeProposal, setActiveProposal] = useState<PlanRevisionProposal | null>(null);
  const [revisionModalOpen, setRevisionModalOpen] = useState(false);

  const [budgetItemModalOpen, setBudgetItemModalOpen] = useState(false);
  const [editingBudgetItem, setEditingBudgetItem] = useState<BudgetItem | null>(null);

  const [taskModalOpen, setTaskModalOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<TaskItem | null>(null);

  const [vendorModalOpen, setVendorModalOpen] = useState(false);
  const [editingVendor, setEditingVendor] = useState<VendorItem | null>(null);

  const [impactModalOpen, setImpactModalOpen] = useState(false);
  const [taskViewMode, setTaskViewMode] = useState<'checklist' | 'timeline'>('checklist');
  const [vendorViewMode, setVendorViewMode] = useState<'directory' | 'matrix'>('directory');

  // Confirm delete dialog state
  const [confirmDialog, setConfirmDialog] = useState<{
    isOpen: boolean;
    title: string;
    message: string;
    onConfirm: () => void;
  }>({
    isOpen: false,
    title: '',
    message: '',
    onConfirm: () => {},
  });

  // Filter states
  const [taskFilter, setTaskFilter] = useState<'ALL' | 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE'>('ALL');

  // Load initial user session
  useEffect(() => {
    async function initAuth() {
      const token = api.getToken();
      if (token) {
        try {
          const user = await api.getMe();
          setCurrentUser(user);
        } catch {
          api.logout();
          setCurrentUser(null);
        }
      }
      setAuthLoading(false);
    }
    initAuth();
  }, []);

  // Fetch events list
  const fetchEvents = useCallback(async () => {
    if (!currentUser) return;
    try {
      const list = await api.listEvents();
      setEvents(list);
      if (list.length > 0) {
        if (!selectedEventId || !list.some((e) => e.id === selectedEventId)) {
          setSelectedEventId(list[0].id);
        }
      } else {
        setSelectedEventId(null);
        setSelectedEvent(null);
      }
    } catch (err: unknown) {
      if (err instanceof Error) {
        setAlertMessage({ type: 'error', text: err.message });
      }
    }
  }, [currentUser, selectedEventId]);

  useEffect(() => {
    if (currentUser) {
      fetchEvents();
    } else {
      setEvents([]);
      setSelectedEventId(null);
      setSelectedEvent(null);
    }
  }, [currentUser, fetchEvents]);

  // Load selected event details and related domain objects
  const loadEventDetails = useCallback(async () => {
    if (!selectedEventId || !currentUser) return;
    setLoadingEvent(true);
    setAlertMessage(null);

    try {
      const [ev, bg, tk, vn, rev] = await Promise.all([
        api.getEvent(selectedEventId),
        api.getBudget(selectedEventId),
        api.getTasks(selectedEventId),
        api.getVendors(selectedEventId),
        api.getRevisions(selectedEventId),
      ]);

      setSelectedEvent(ev);
      setBudget(bg);
      setTaskOverview(tk);
      setVendors(vn);
      setRevisions(rev);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setAlertMessage({ type: 'error', text: err.message });
      }
    } finally {
      setLoadingEvent(false);
    }
  }, [selectedEventId, currentUser]);

  useEffect(() => {
    loadEventDetails();
  }, [loadEventDetails]);

  // AI Plan generation handler
  const handleGeneratePlan = async () => {
    if (!selectedEventId) return;
    setPlanGenerating(true);
    setPlanError(null);
    setAlertMessage(null);

    try {
      const plan = await api.generatePlan(selectedEventId, planNotes);
      setGeneratedPlan(plan);
      setAlertMessage({ type: 'success', text: 'Plan generated and saved successfully!' });
      await loadEventDetails();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Plan generation failed. Please try again.';
      setPlanError(msg);
      setAlertMessage({ type: 'error', text: msg });
    } finally {
      setPlanGenerating(false);
    }
  };

  // AI Plan revision proposal handler
  const handleProposeRevision = async () => {
    if (!selectedEventId || !revisionPrompt.trim()) return;
    setRevisionProposing(true);
    setAlertMessage(null);

    try {
      const proposal = await api.proposeRevision(selectedEventId, revisionPrompt);
      setActiveProposal(proposal);
      setRevisionModalOpen(true);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setAlertMessage({ type: 'error', text: err.message });
      }
    } finally {
      setRevisionProposing(false);
    }
  };

  // Delete event with explicit confirmation
  const handleDeleteEvent = () => {
    if (!selectedEvent) return;
    setConfirmDialog({
      isOpen: true,
      title: 'Delete this event?',
      message: `Are you sure you want to delete "${selectedEvent.name}"? This action cannot be undone and will permanently remove all associated budget items, tasks, and revision history.`,
      onConfirm: async () => {
        try {
          await api.deleteEvent(selectedEvent.id);
          setConfirmDialog((prev) => ({ ...prev, isOpen: false }));
          setAlertMessage({ type: 'info', text: 'Event was deleted.' });
          fetchEvents();
        } catch (err: unknown) {
          if (err instanceof Error) {
            setAlertMessage({ type: 'error', text: err.message });
          }
        }
      },
    });
  };

  // Toggle task status
  const handleToggleTaskStatus = async (task: TaskItem) => {
    if (!selectedEventId) return;
    const nextStatus = task.status === 'COMPLETED' ? 'TODO' : 'COMPLETED';
    try {
      await api.updateTask(selectedEventId, task.id, { status: nextStatus });
      loadEventDetails();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setAlertMessage({ type: 'error', text: err.message });
      }
    }
  };

  // Accept suggested alternative date for overdue task
  const handleAcceptAlternativeDate = async (task: TaskItem) => {
    if (!selectedEventId || !task.suggestedAlternativeDate) return;
    try {
      await api.updateTask(selectedEventId, task.id, { dueDate: task.suggestedAlternativeDate });
      setAlertMessage({ type: 'success', text: `Updated task due date to ${task.suggestedAlternativeDate}` });
      loadEventDetails();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setAlertMessage({ type: 'error', text: err.message });
      }
    }
  };

  // Delete budget item with explicit confirmation
  const handleDeleteBudgetItem = (item: BudgetItem) => {
    if (!selectedEventId) return;
    setConfirmDialog({
      isOpen: true,
      title: 'Delete budget line item?',
      message: `Delete "${item.category}${item.description ? ' - ' + item.description : ''}"? This will update the server-calculated total budget immediately.`,
      onConfirm: async () => {
        try {
          await api.deleteBudgetItem(selectedEventId, item.id);
          setConfirmDialog((prev) => ({ ...prev, isOpen: false }));
          loadEventDetails();
        } catch (err: unknown) {
          if (err instanceof Error) {
            setAlertMessage({ type: 'error', text: err.message });
          }
        }
      },
    });
  };

  // Delete task with explicit confirmation
  const handleDeleteTask = (task: TaskItem) => {
    if (!selectedEventId) return;
    setConfirmDialog({
      isOpen: true,
      title: 'Delete task?',
      message: `Are you sure you want to delete task "${task.title}"?`,
      onConfirm: async () => {
        try {
          await api.deleteTask(selectedEventId, task.id);
          setConfirmDialog((prev) => ({ ...prev, isOpen: false }));
          loadEventDetails();
        } catch (err: unknown) {
          if (err instanceof Error) {
            setAlertMessage({ type: 'error', text: err.message });
          }
        }
      },
    });
  };

  // Delete vendor with explicit confirmation
  const handleDeleteVendor = (vendor: VendorItem) => {
    if (!selectedEventId) return;
    setConfirmDialog({
      isOpen: true,
      title: 'Delete vendor record?',
      message: `Delete vendor record for "${vendor.name || vendor.category}"?`,
      onConfirm: async () => {
        try {
          await api.deleteVendor(selectedEventId, vendor.id);
          setConfirmDialog((prev) => ({ ...prev, isOpen: false }));
          loadEventDetails();
        } catch (err: unknown) {
          if (err instanceof Error) {
            setAlertMessage({ type: 'error', text: err.message });
          }
        }
      },
    });
  };

  // Book vendor and lock quote into budget actuals
  const handleBookVendor = async (vendor: VendorItem) => {
    if (!selectedEventId) return;
    try {
      await api.bookVendor(selectedEventId, vendor.id);
      setAlertMessage({
        type: 'success',
        text: `Vendor "${vendor.name || vendor.category}" booked! Quote linked to budget actuals and locked as confirmed expenditure.`,
      });
      loadEventDetails();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setAlertMessage({ type: 'error', text: err.message });
      }
    }
  };

  // Calculate days remaining to event
  const getDaysRemaining = (eventDateStr: string | null) => {
    if (!eventDateStr) return null;
    const target = new Date(eventDateStr);
    const now = new Date();
    target.setHours(0, 0, 0, 0);
    now.setHours(0, 0, 0, 0);
    const diff = Math.ceil((target.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
    return diff;
  };

  const daysRemaining = selectedEvent ? getDaysRemaining(selectedEvent.eventDate) : null;

  // Filter tasks
  const filteredTasks = (taskOverview?.tasks || []).filter((task) => {
    if (taskFilter === 'OVERDUE') return task.isOverdue;
    if (taskFilter === 'ALL') return true;
    return task.status === taskFilter;
  });

  // Not signed in state
  if (!authLoading && !currentUser) {
    return (
      <div className="app-shell" style={{ justifyContent: 'center', alignItems: 'center' }}>
        <div
          className="section-card"
          style={{ maxWidth: '480px', width: '100%', textAlign: 'center', padding: '40px 32px' }}
        >
          <div className="brand" style={{ justifyContent: 'center', marginBottom: '16px' }}>
            <span className="brand-mark">
              <CalendarHeart size={22} />
            </span>
            <span>gatherly</span>
          </div>
          <h2 style={{ fontFamily: 'var(--font-heading)', fontSize: '22px', marginBottom: '8px' }}>
            Thoughtful Event Planning
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '14px', marginBottom: '24px', lineHeight: '1.6' }}>
            Transform your celebration ideas into an actionable plan with smart budgets, realistic timelines, and clear next steps.
          </p>
          <button
            type="button"
            className="btn-primary"
            style={{ width: '100%', justifyContent: 'center', padding: '12px' }}
            onClick={() => setAuthModalOpen(true)}
          >
            <LogIn size={18} />
            Sign In or Create Workspace
          </button>
        </div>
        <AuthModal
          isOpen={authModalOpen}
          onClose={() => setAuthModalOpen(false)}
          onSuccess={(u) => {
            setCurrentUser(u);
            fetchEvents();
          }}
        />
      </div>
    );
  }

  return (
    <div className="app-shell">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">
            <CalendarHeart size={22} />
          </span>
          <span>gatherly</span>
          <span className="brand-badge">PRO</span>
        </div>

        {/* Event Selector */}
        <div className="event-selector-card">
          <div className="event-selector-label">
            <span>ACTIVE CELEBRATION</span>
            <button
              type="button"
              className="btn-subtle"
              style={{ padding: '2px 4px' }}
              onClick={() => {
                setEditingEvent(null);
                setNewEventModalOpen(true);
              }}
              title="Create new event"
            >
              <Plus size={14} />
            </button>
          </div>
          {events.length > 0 ? (
            <select
              className="event-select-dropdown"
              value={selectedEventId || ''}
              onChange={(e) => setSelectedEventId(Number(e.target.value))}
            >
              {events.map((ev) => (
                <option key={ev.id} value={ev.id}>
                  {ev.name} ({ev.eventType})
                </option>
              ))}
            </select>
          ) : (
            <button
              type="button"
              className="btn-secondary"
              style={{ width: '100%', fontSize: '12px', justifyContent: 'center' }}
              onClick={() => setNewEventModalOpen(true)}
            >
              <Plus size={14} /> Start your first event
            </button>
          )}
        </div>

        <p className="sidebar-section-title">PLANNING SUITE</p>
        <nav className="nav-list">
          <button
            type="button"
            className={`nav-item ${activeTab === 'overview' ? 'active' : ''}`}
            onClick={() => setActiveTab('overview')}
          >
            <CalendarHeart size={18} />
            <span>Overview</span>
          </button>

          <button
            type="button"
            className={`nav-item ${activeTab === 'plan' ? 'active' : ''}`}
            onClick={() => setActiveTab('plan')}
          >
            <Sparkles size={18} />
            <span>AI Plan Generator</span>
          </button>

          <button
            type="button"
            className={`nav-item ${activeTab === 'revisions' ? 'active' : ''}`}
            onClick={() => setActiveTab('revisions')}
          >
            <History size={18} />
            <span>Revision Assistant</span>
            {revisions.length > 0 && <span className="nav-item-count">v{revisions[0].versionNumber}</span>}
          </button>

          <button
            type="button"
            className={`nav-item ${activeTab === 'budget' ? 'active' : ''}`}
            onClick={() => setActiveTab('budget')}
          >
            <Wallet size={18} />
            <span>Budget</span>
            {budget && budget.isOverBudget && (
              <span className="badge badge-danger" style={{ marginLeft: 'auto', fontSize: '9px', padding: '1px 5px' }}>
                OVER
              </span>
            )}
          </button>

          <button
            type="button"
            className={`nav-item ${activeTab === 'checklist' ? 'active' : ''}`}
            onClick={() => setActiveTab('checklist')}
          >
            <ListChecks size={18} />
            <span>Checklist & Tasks</span>
            {taskOverview && taskOverview.overdueTasks > 0 && (
              <span className="badge badge-danger" style={{ marginLeft: 'auto', fontSize: '9px', padding: '1px 5px' }}>
                {taskOverview.overdueTasks} late
              </span>
            )}
          </button>

          <button
            type="button"
            className={`nav-item ${activeTab === 'vendors' ? 'active' : ''}`}
            onClick={() => setActiveTab('vendors')}
          >
            <Store size={18} />
            <span>Vendor Organizer</span>
            {vendors.length > 0 && <span className="nav-item-count">{vendors.length}</span>}
          </button>
        </nav>

        {/* User profile & session */}
        <div className="sidebar-bottom">
          <div className="user-profile-badge">
            <div className="user-avatar">{currentUser?.displayName?.charAt(0).toUpperCase() || 'U'}</div>
            <div className="user-info">
              <span className="user-name">{currentUser?.displayName}</span>
              <span className="user-email">{currentUser?.email}</span>
            </div>
            <button
              type="button"
              className="btn-subtle"
              style={{ marginLeft: 'auto', padding: '6px' }}
              onClick={() => {
                api.logout();
                setCurrentUser(null);
              }}
              title="Sign out"
            >
              <LogOut size={16} />
            </button>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="main-wrapper">
        {/* Topbar */}
        <header className="topbar">
          <div className="topbar-breadcrumbs">
            <span>Workspace</span>
            <span className="breadcrumb-separator">/</span>
            <span>{selectedEvent?.name || 'No event selected'}</span>
            <span className="breadcrumb-separator">/</span>
            <span className="breadcrumb-current" style={{ textTransform: 'capitalize' }}>
              {activeTab}
            </span>
            {loadingEvent && (
              <span className="badge badge-estimate" style={{ marginLeft: '10px', display: 'inline-flex', alignItems: 'center', gap: '4px', fontSize: '11px', padding: '2px 8px' }}>
                <RefreshCw size={11} className="spin" /> Updating...
              </span>
            )}
          </div>

          <div className="topbar-actions">
            {selectedEvent && (
              <>
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setImpactModalOpen(true)}
                  title="Simulate impact of changing guest count, celebration date, or budget"
                >
                  <TrendingUp size={16} />
                  <span>Impact Simulator</span>
                </button>
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => {
                    setEditingEvent(selectedEvent);
                    setNewEventModalOpen(true);
                  }}
                  title="Edit event details"
                >
                  <Edit2 size={16} />
                  <span>Edit Event</span>
                </button>
                <button
                  type="button"
                  className="btn-subtle"
                  style={{ color: 'var(--badge-danger-text)' }}
                  onClick={handleDeleteEvent}
                  title="Delete event"
                >
                  <Trash2 size={16} />
                  <span>Delete</span>
                </button>
              </>
            )}
            <button
              type="button"
              className="btn-primary"
              onClick={() => {
                setEditingEvent(null);
                setNewEventModalOpen(true);
              }}
            >
              <Plus size={16} />
              <span>New Event</span>
            </button>
          </div>
        </header>

        {/* Body content */}
        <main className="content-area">
          {/* Notification Alert */}
          {alertMessage && (
            <div className={`alert-banner ${alertMessage.type}`}>
              {alertMessage.type === 'error' && <AlertTriangle size={18} />}
              {alertMessage.type === 'success' && <CheckCircle2 size={18} />}
              <span>{alertMessage.text}</span>
              <button
                type="button"
                className="btn-subtle"
                style={{ marginLeft: 'auto', padding: '2px' }}
                onClick={() => setAlertMessage(null)}
              >
                <X size={14} />
              </button>
            </div>
          )}

          {/* Empty state: No events created yet */}
          {events.length === 0 ? (
            <div className="section-card empty-state">
              <div className="empty-state-icon">
                <CalendarHeart size={28} />
              </div>
              <h3>No events planned yet</h3>
              <p>
                Get started by creating your first celebration brief. Gatherly will generate an editable budget,
                checklist, and personalized ideas.
              </p>
              <button
                type="button"
                className="btn-primary"
                onClick={() => {
                  setEditingEvent(null);
                  setNewEventModalOpen(true);
                }}
              >
                <Plus size={16} /> Create Your First Event
              </button>
            </div>
          ) : selectedEvent ? (
            <>
              {/* Event Hero Banner */}
              <section className="event-hero">
                <div>
                  <div className="event-hero-meta">
                    <span className="event-type-pill">{selectedEvent.eventType}</span>
                    <span style={{ fontSize: '12px', color: 'rgba(255, 255, 255, 0.7)' }}>
                      Created on {new Date(selectedEvent.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                  <h1 className="event-hero-title">{selectedEvent.name}</h1>
                  <div className="event-hero-details">
                    {selectedEvent.eventDate && (
                      <span className="hero-detail-item">
                        <Calendar size={15} />
                        {new Date(selectedEvent.eventDate).toLocaleDateString(undefined, {
                          weekday: 'short',
                          year: 'numeric',
                          month: 'short',
                          day: 'numeric',
                        })}
                      </span>
                    )}
                    {selectedEvent.location && (
                      <span className="hero-detail-item">
                        <MapPin size={15} />
                        {selectedEvent.location}
                      </span>
                    )}
                    {selectedEvent.guestCount && (
                      <span className="hero-detail-item">
                        <Users size={15} />
                        {selectedEvent.guestCount} guests
                      </span>
                    )}
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '28px' }}>
                  <div className="hero-art" aria-hidden="true">
                    <div className="orbit" />
                    <div className="orbit orbit-two" />
                    <Sparkles className="sparkle s1" size={24} />
                    <Sparkles className="sparkle s2" size={16} />
                    <div className="hero-icon">
                      <CalendarHeart size={44} />
                    </div>
                  </div>

                  {daysRemaining !== null && (
                    <div className="hero-countdown">
                      <div className="hero-countdown-number">
                        {daysRemaining >= 0 ? daysRemaining : 0}
                      </div>
                      <div className="hero-countdown-label">
                        {daysRemaining >= 0 ? 'Days Until Celebration' : 'Event Completed'}
                      </div>
                    </div>
                  )}
                </div>
              </section>

              {/* KPI Metrics Strip */}
              <section className="kpi-grid">
                <div className="kpi-card">
                  <div className="kpi-header">
                    <span>TARGET BUDGET</span>
                    <div className="kpi-icon-wrap">
                      <Wallet size={16} />
                    </div>
                  </div>
                  <div className="kpi-value">
                    {selectedEvent.currency} {selectedEvent.totalBudget.toLocaleString()}
                  </div>
                  <div className="kpi-caption">Fixed target allocation</div>
                </div>

                <div className={`kpi-card ${budget?.isOverBudget ? 'alert' : ''}`}>
                  <div className="kpi-header">
                    <span>ESTIMATED SPEND</span>
                    <div className="kpi-icon-wrap">
                      <Sparkles size={16} />
                    </div>
                  </div>
                  <div className="kpi-value">
                    {selectedEvent.currency} {(budget?.totalEstimated || 0).toLocaleString()}
                  </div>
                  <div className="kpi-caption">
                    {budget?.isOverBudget
                      ? `Over budget by ${selectedEvent.currency} ${Math.abs(budget.remainingBudget).toLocaleString()}`
                      : `${selectedEvent.currency} ${(budget?.remainingBudget || 0).toLocaleString()} remaining`}
                  </div>
                </div>

                <div className="kpi-card">
                  <div className="kpi-header">
                    <span>CONFIRMED ACTUAL</span>
                    <div className="kpi-icon-wrap">
                      <CheckCircle2 size={16} />
                    </div>
                  </div>
                  <div className="kpi-value">
                    {selectedEvent.currency} {(budget?.totalActual || 0).toLocaleString()}
                  </div>
                  <div className="kpi-caption">
                    {budget?.confirmedCount || 0} of {budget?.totalItemsCount || 0} items confirmed
                  </div>
                </div>

                <div className="kpi-card">
                  <div className="kpi-header">
                    <span>TASKS COMPLETED</span>
                    <div className="kpi-icon-wrap">
                      <ListChecks size={16} />
                    </div>
                  </div>
                  <div className="kpi-value">{taskOverview?.completionPercentage || 0}%</div>
                  <div className="progress-bar-bg">
                    <div
                      className="progress-bar-fill"
                      style={{ width: `${taskOverview?.completionPercentage || 0}%` }}
                    />
                  </div>
                  <div className="kpi-caption">
                    {taskOverview?.completedTasks || 0} of {taskOverview?.totalTasks || 0} tasks done
                  </div>
                </div>
              </section>

              {/* TAB 1: OVERVIEW */}
              {activeTab === 'overview' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
                  {/* Over-budget alert if applicable */}
                  {budget?.isOverBudget && (
                    <div className="alert-banner warning">
                      <AlertTriangle size={18} style={{ flexShrink: 0 }} />
                      <div>
                        <strong>Budget Notice:</strong> The total estimated allocations exceed your target budget by{' '}
                        {selectedEvent.currency} {Math.abs(budget.remainingBudget).toLocaleString()}. Consider using the
                        Revision Assistant to trim discretionary expenses.
                      </div>
                    </div>
                  )}

                  {/* Overdue tasks alert if applicable */}
                  {taskOverview && taskOverview.overdueTasks > 0 && (
                    <div className="alert-banner error">
                      <Clock size={18} style={{ flexShrink: 0 }} />
                      <div>
                        <strong>Upcoming Deadlines:</strong> You have {taskOverview.overdueTasks} task(s) past their
                        suggested due date.
                      </div>
                    </div>
                  )}

                  {/* Quick Shortcuts & Priorities Card */}
                  <div className="section-card">
                    <div className="section-header">
                      <div className="section-title-wrap">
                        <h2>Event Brief & Key Priorities</h2>
                        <p>Guidelines shaping your celebration's budget and schedule.</p>
                      </div>
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button
                          type="button"
                          className="btn-secondary"
                          onClick={() => {
                            setEditingEvent(selectedEvent);
                            setNewEventModalOpen(true);
                          }}
                        >
                          <Edit2 size={15} />
                          Edit Brief
                        </button>
                        <button
                          type="button"
                          className="btn-primary"
                          onClick={() => setActiveTab('plan')}
                        >
                          <Sparkles size={15} />
                          Open AI Planner
                        </button>
                      </div>
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: '16px' }}>
                      <div style={{ backgroundColor: 'var(--bg-subtle)', padding: '14px', borderRadius: 'var(--radius-md)' }}>
                        <div style={{ fontSize: '11px', fontWeight: 700, color: 'var(--text-subtle)', textTransform: 'uppercase' }}>
                          Style & Atmosphere
                        </div>
                        <p style={{ fontSize: '13px', marginTop: '6px', color: 'var(--text-main)' }}>
                          {selectedEvent.stylePreferences || 'No specific style stated.'}
                        </p>
                      </div>

                      <div style={{ backgroundColor: 'var(--bg-subtle)', padding: '14px', borderRadius: 'var(--radius-md)' }}>
                        <div style={{ fontSize: '11px', fontWeight: 700, color: 'var(--text-subtle)', textTransform: 'uppercase' }}>
                          Dietary & Hospitality
                        </div>
                        <p style={{ fontSize: '13px', marginTop: '6px', color: 'var(--text-main)' }}>
                          {selectedEvent.dietaryRequirements || 'Standard catering.'}
                        </p>
                      </div>

                      <div style={{ backgroundColor: 'var(--bg-subtle)', padding: '14px', borderRadius: 'var(--radius-md)' }}>
                        <div style={{ fontSize: '11px', fontWeight: 700, color: 'var(--text-subtle)', textTransform: 'uppercase' }}>
                          Accessibility & Priorities
                        </div>
                        <p style={{ fontSize: '13px', marginTop: '6px', color: 'var(--text-main)' }}>
                          {selectedEvent.priorities || selectedEvent.accessibilityNeeds || 'Balanced experience.'}
                        </p>
                      </div>
                    </div>
                  </div>

                  {/* Upcoming Tasks Snapshot */}
                  <div className="section-card">
                    <div className="section-header">
                      <div className="section-title-wrap">
                        <h2>Immediate Next Steps</h2>
                        <p>Priority checklist items needing your attention.</p>
                      </div>
                      <button
                        type="button"
                        className="btn-secondary"
                        onClick={() => setActiveTab('checklist')}
                      >
                        View Full Checklist ({taskOverview?.totalTasks || 0})
                      </button>
                    </div>

                    {taskOverview && taskOverview.tasks.length > 0 ? (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                        {taskOverview.tasks.slice(0, 4).map((task) => (
                          <div
                            key={task.id}
                            className={`checklist-item ${task.status === 'COMPLETED' ? 'completed' : ''}`}
                          >
                            <input
                              type="checkbox"
                              className="task-checkbox"
                              checked={task.status === 'COMPLETED'}
                              onChange={() => handleToggleTaskStatus(task)}
                            />
                            <div style={{ flex: 1 }}>
                              <span className="task-title" style={{ fontSize: '13px', fontWeight: 600 }}>
                                {task.title}
                              </span>
                              {task.description && (
                                <p style={{ fontSize: '11px', color: 'var(--text-subtle)', marginTop: '2px' }}>
                                  {task.description}
                                </p>
                              )}
                            </div>
                            {task.isOverdue && (
                              <span className="badge badge-danger">LATE</span>
                            )}
                            <span className="badge badge-neutral">{task.priority}</span>
                            {task.dueDate && (
                              <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                                {task.dueDate}
                              </span>
                            )}
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="empty-state" style={{ padding: '24px' }}>
                        <p>No tasks created yet. Generate an AI plan or add tasks manually.</p>
                      </div>
                    )}
                  </div>

                  {/* Planning Tools Grid */}
                  <div className="section-card">
                    <div className="section-header">
                      <div className="section-title-wrap">
                        <h2>Event Planning Suite</h2>
                        <p>Essential modules for orchestrating your celebration from start to finish.</p>
                      </div>
                    </div>
                    <div className="tools-grid">
                      <div className="tool-card" onClick={() => setActiveTab('plan')} role="button" tabIndex={0}>
                        <div className="tool-icon green">
                          <Sparkles size={20} />
                        </div>
                        <div>
                          <h4 style={{ fontWeight: 600, fontSize: '14px', marginBottom: '3px' }}>AI Plan Generator</h4>
                          <p style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Generate structured budgets, theme concepts, and timeline from your brief.</p>
                        </div>
                      </div>

                      <div className="tool-card" onClick={() => setActiveTab('budget')} role="button" tabIndex={0}>
                        <div className="tool-icon peach">
                          <Wallet size={20} />
                        </div>
                        <div>
                          <h4 style={{ fontWeight: 600, fontSize: '14px', marginBottom: '3px' }}>Budget Manager</h4>
                          <p style={{ fontSize: '12px', color: 'var(--text-muted)' }}>{selectedEvent.currency} {selectedEvent.totalBudget.toLocaleString()} target · Track estimated vs confirmed actuals.</p>
                        </div>
                      </div>

                      <div className="tool-card" onClick={() => setActiveTab('checklist')} role="button" tabIndex={0}>
                        <div className="tool-icon lilac">
                          <ListChecks size={20} />
                        </div>
                        <div>
                          <h4 style={{ fontWeight: 600, fontSize: '14px', marginBottom: '3px' }}>Timeline & Checklist</h4>
                          <p style={{ fontSize: '12px', color: 'var(--text-muted)' }}>{taskOverview?.completedTasks || 0}/{taskOverview?.totalTasks || 0} tasks complete · Proactive date adjustments.</p>
                        </div>
                      </div>

                      <div className="tool-card" onClick={() => setActiveTab('vendors')} role="button" tabIndex={0}>
                        <div className="tool-icon blue">
                          <Store size={20} />
                        </div>
                        <div>
                          <h4 style={{ fontWeight: 600, fontSize: '14px', marginBottom: '3px' }}>Vendor Organizer</h4>
                          <p style={{ fontSize: '12px', color: 'var(--text-muted)' }}>{vendors.length} vendor contacts · Manage quotes, booking status, and notes.</p>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Celebration Archetypes Showcase */}
                  <div className="section-card">
                    <div className="section-header">
                      <div className="section-title-wrap">
                        <h2>Celebration Archetypes</h2>
                        <p>Tailored planning dynamics and templates for every occasion style.</p>
                      </div>
                    </div>
                    <div className="event-types-row">
                      <div
                        className={`event-type-card ${selectedEvent.eventType === 'Wedding' ? 'selected' : ''}`}
                        onClick={() => {
                          if (selectedEvent.eventType !== 'Wedding') {
                            setEditingEvent({ ...selectedEvent, eventType: 'Wedding' });
                            setNewEventModalOpen(true);
                          }
                        }}
                      >
                        <div className="event-emoji emoji-wedding">💍</div>
                        <h4 style={{ fontSize: '13px', fontWeight: 600 }}>Wedding & Reception</h4>
                        <p style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Multi-phase timelines, vendor coordination, decor, and guest hospitality.</p>
                      </div>

                      <div
                        className={`event-type-card ${selectedEvent.eventType === 'Birthday' ? 'selected' : ''}`}
                        onClick={() => {
                          if (selectedEvent.eventType !== 'Birthday') {
                            setEditingEvent({ ...selectedEvent, eventType: 'Birthday' });
                            setNewEventModalOpen(true);
                          }
                        }}
                      >
                        <div className="event-emoji emoji-birthday">🎂</div>
                        <h4 style={{ fontSize: '13px', fontWeight: 600 }}>Birthday Celebration</h4>
                        <p style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Thematic styling, entertainment, personalized menus, and party favors.</p>
                      </div>

                      <div
                        className={`event-type-card ${selectedEvent.eventType === 'Community event' ? 'selected' : ''}`}
                        onClick={() => {
                          if (selectedEvent.eventType !== 'Community event') {
                            setEditingEvent({ ...selectedEvent, eventType: 'Community event' });
                            setNewEventModalOpen(true);
                          }
                        }}
                      >
                        <div className="event-emoji emoji-community">🌿</div>
                        <h4 style={{ fontSize: '13px', fontWeight: 600 }}>Community Gathering</h4>
                        <p style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Crowd management, public venue logistics, accessibility, and volunteer roles.</p>
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* TAB 2: AI PLAN GENERATOR */}
              {activeTab === 'plan' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
                  <div className="section-card">
                    <div className="section-header">
                      <div className="section-title-wrap">
                        <h2>GenAI Plan Generator</h2>
                        <p>Shape an editable, structured plan tailored to your budget and event brief.</p>
                      </div>
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="plan-notes-input">
                        Event Brief & Special Instructions
                      </label>
                      <textarea
                        id="plan-notes-input"
                        className="form-textarea"
                        value={planNotes}
                        onChange={(e) => setPlanNotes(e.target.value)}
                        placeholder="e.g. Please prioritize local organic catering, include interactive lawn games, and keep decor rustic but affordable."
                      />
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '16px' }}>
                      <span style={{ fontSize: '12px', color: 'var(--text-subtle)' }}>
                        All generated budget amounts are estimates and must be verified before booking.
                      </span>
                      <button
                        type="button"
                        className="btn-primary"
                        onClick={handleGeneratePlan}
                        disabled={planGenerating}
                      >
                        <Sparkles size={16} />
                        {planGenerating ? 'Generating Structured Plan...' : 'Generate / Refresh Plan'}
                      </button>
                    </div>
                  </div>

                  {/* Loading State */}
                  {planGenerating && (
                    <div className="section-card" style={{ textAlign: 'center', padding: '48px 24px' }}>
                      <div
                        style={{
                          display: 'inline-flex',
                          padding: '16px',
                          borderRadius: '50%',
                          backgroundColor: 'var(--bg-sage-light)',
                          color: 'var(--primary-forest)',
                          marginBottom: '16px',
                        }}
                      >
                        <RefreshCw size={36} className="spin-slow" />
                      </div>
                      <h3 style={{ fontFamily: 'var(--font-heading)', fontSize: '20px', marginBottom: '8px' }}>
                        Gatherly AI is Shaping Your Celebration Plan
                      </h3>
                      <p style={{ color: 'var(--text-muted)', fontSize: '13px', maxWidth: '480px', margin: '0 auto', lineHeight: '1.6' }}>
                        Synthesizing event constraints, calculating deterministic category allocations with BigDecimal, sequencing timeline milestones, and generating personalized ideas...
                      </p>
                    </div>
                  )}

                  {/* Error State with Retry Button */}
                  {planError && !planGenerating && (
                    <div className="alert-banner error" style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <AlertTriangle size={18} />
                        <strong>Plan Generation Notice</strong>
                      </div>
                      <p style={{ fontSize: '13px', margin: 0 }}>{planError}</p>
                      <div style={{ display: 'flex', gap: '10px', marginTop: '6px' }}>
                        <button
                          type="button"
                          className="btn-primary"
                          onClick={handleGeneratePlan}
                          style={{ padding: '6px 14px', fontSize: '12px' }}
                        >
                          <RefreshCw size={14} /> Retry Generation
                        </button>
                      </div>
                    </div>
                  )}

                  {/* Display Generated Plan Result if exists */}
                  {generatedPlan && !planGenerating && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                      {/* Development Mock Notice Banner */}
                      {generatedPlan.providerNotice && (
                        <div className="alert-banner warning" style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                          <Lightbulb size={18} style={{ flexShrink: 0 }} />
                          <div style={{ fontSize: '12px' }}>
                            <strong>Development Mock Engine:</strong> {generatedPlan.providerNotice}
                          </div>
                        </div>
                      )}

                      {/* Over-Budget Alert & Trade-Off Suggestions */}
                      {generatedPlan.isOverBudget && (
                        <div className="alert-banner warning" style={{ display: 'flex', flexDirection: 'column', gap: '10px', padding: '18px' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <AlertTriangle size={18} />
                            <strong>Budget Advisory: Target Exceeded</strong>
                          </div>
                          <p style={{ fontSize: '13px', margin: 0 }}>{generatedPlan.budgetWarning}</p>
                          {generatedPlan.tradeOffSuggestions && generatedPlan.tradeOffSuggestions.length > 0 && (
                            <div style={{ marginTop: '4px' }}>
                              <strong style={{ fontSize: '12px', textTransform: 'uppercase', color: 'var(--text-main)' }}>
                                Recommended Trade-Offs to Fit Budget:
                              </strong>
                              <ul style={{ paddingLeft: '18px', marginTop: '6px', fontSize: '12px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                                {generatedPlan.tradeOffSuggestions.map((to, idx) => (
                                  <li key={idx}>{to}</li>
                                ))}
                              </ul>
                            </div>
                          )}
                        </div>
                      )}

                      {/* Clarifying Questions from Assistant if Essential Details were Incomplete */}
                      {generatedPlan.followUpQuestions && generatedPlan.followUpQuestions.length > 0 && (
                        <div className="section-card" style={{ borderLeft: '4px solid var(--accent-sage)' }}>
                          <div className="section-header" style={{ marginBottom: '10px' }}>
                            <div className="section-title-wrap">
                              <h3>Clarifying Questions from Gatherly</h3>
                              <p>Answering these details will refine subsequent plan revisions and vendor recommendations.</p>
                            </div>
                          </div>
                          <ul style={{ paddingLeft: '20px', fontSize: '13px', color: 'var(--text-main)', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                            {generatedPlan.followUpQuestions.map((q, idx) => (
                              <li key={idx}><strong>{q}</strong></li>
                            ))}
                          </ul>
                        </div>
                      )}

                      {/* Assumptions Card */}
                      {generatedPlan.assumptions && generatedPlan.assumptions.length > 0 && (
                        <div className="section-card">
                          <h4 style={{ fontFamily: 'var(--font-heading)', fontSize: '13px', marginBottom: '8px', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                            Assumptions Made in This Plan
                          </h4>
                          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '8px' }}>
                            {generatedPlan.assumptions.map((asm, idx) => (
                              <div key={idx} style={{ padding: '8px 12px', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-sm)', fontSize: '12px' }}>
                                • {asm}
                              </div>
                            ))}
                          </div>
                        </div>
                      )}

                      {/* Plan Summary & Theme Concepts */}
                      <div className="section-card">
                        <h3 style={{ fontFamily: 'var(--font-heading)', fontSize: '16px', marginBottom: '8px' }}>
                          Plan Vision & Summary
                        </h3>
                        <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: '1.6' }}>
                          {generatedPlan.summary}
                        </p>

                        {generatedPlan.themeSuggestions && generatedPlan.themeSuggestions.length > 0 && (
                          <div style={{ marginTop: '16px' }}>
                            <div style={{ fontSize: '11px', fontWeight: 700, color: 'var(--text-subtle)', textTransform: 'uppercase' }}>
                              Recommended Aesthetic Concepts
                            </div>
                            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginTop: '8px' }}>
                              {generatedPlan.themeSuggestions.map((theme, i) => (
                                <span key={i} className="badge badge-neutral" style={{ fontSize: '12px', padding: '4px 10px' }}>
                                  {theme}
                                </span>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>

                      {/* Phased Planning Timeline */}
                      {generatedPlan.timeline && generatedPlan.timeline.length > 0 && (
                        <div className="section-card">
                          <div className="section-header">
                            <div className="section-title-wrap">
                              <h2>Phased Planning Timeline</h2>
                              <p>Recommended sequence of milestones leading up to celebration day.</p>
                            </div>
                          </div>
                          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '14px' }}>
                            {generatedPlan.timeline.map((phase, pIdx) => (
                              <div
                                key={pIdx}
                                style={{
                                  border: '1px solid var(--border-subtle)',
                                  borderRadius: 'var(--radius-md)',
                                  padding: '16px',
                                  backgroundColor: 'var(--bg-subtle)',
                                }}
                              >
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: '8px' }}>
                                  <h4 style={{ fontWeight: 700, fontSize: '13px', color: 'var(--primary-forest)' }}>
                                    {phase.phaseName}
                                  </h4>
                                  <span className="badge badge-neutral" style={{ fontSize: '10px' }}>
                                    {phase.timeframe}
                                  </span>
                                </div>
                                <ul style={{ paddingLeft: '16px', fontSize: '12px', color: 'var(--text-main)', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                                  {phase.keyMilestones.map((ms, mIdx) => (
                                    <li key={mIdx}>{ms}</li>
                                  ))}
                                </ul>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}

                      {/* Proposed Budget Allocations */}
                      {generatedPlan.budgetItems && generatedPlan.budgetItems.length > 0 && (
                        <div className="section-card">
                          <div className="section-header">
                            <div className="section-title-wrap">
                              <h2>Proposed Budget Allocations</h2>
                              <p>Estimates calculated with BigDecimal. All amounts require vendor quotes.</p>
                            </div>
                            <button
                              type="button"
                              className="btn-secondary"
                              onClick={() => setActiveTab('budget')}
                            >
                              <Wallet size={15} /> Open Budget Manager
                            </button>
                          </div>
                          <table className="data-table">
                            <thead>
                              <tr>
                                <th>Category</th>
                                <th>Description</th>
                                <th style={{ textAlign: 'right' }}>Estimated Allocation</th>
                                <th>Status</th>
                              </tr>
                            </thead>
                            <tbody>
                              {generatedPlan.budgetItems.map((b, bIdx) => (
                                <tr key={bIdx}>
                                  <td><strong>{b.category}</strong></td>
                                  <td>{b.description}</td>
                                  <td style={{ textAlign: 'right', fontWeight: 600 }}>
                                    {selectedEvent?.currency} {b.estimatedAmount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                                  </td>
                                  <td>
                                    <span className="badge badge-estimate">ESTIMATE</span>
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                            <tfoot>
                              <tr>
                                <td colSpan={2}><strong>Total Estimated Allocation</strong></td>
                                <td style={{ textAlign: 'right', fontWeight: 700 }}>
                                  {selectedEvent?.currency} {generatedPlan.totalCalculatedBudget.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                                </td>
                                <td>
                                  {generatedPlan.isOverBudget ? (
                                    <span className="badge badge-danger">OVER BUDGET</span>
                                  ) : (
                                    <span className="badge badge-confirmed">FITS BUDGET</span>
                                  )}
                                </td>
                              </tr>
                            </tfoot>
                          </table>
                        </div>
                      )}

                      {/* Creative Ideas */}
                      {generatedPlan.ideas && generatedPlan.ideas.length > 0 && (
                        <div>
                          <div className="section-header">
                            <div className="section-title-wrap">
                              <h2>Personalized Creative Ideas</h2>
                              <p>Inspirations tailored to your guests and atmosphere preferences.</p>
                            </div>
                          </div>
                          <div className="ideas-grid">
                            {generatedPlan.ideas.map((idea, idx) => (
                              <div key={idx} className="idea-card">
                                <span className="idea-card-cat">{idea.category}</span>
                                <h3>{idea.title}</h3>
                                <p>{idea.description}</p>
                                <div className="idea-card-actions">
                                  <button
                                    type="button"
                                    className="btn-subtle"
                                    onClick={async () => {
                                      if (!selectedEventId) return;
                                      try {
                                        await api.createTask(selectedEventId, {
                                          title: `Implement Idea: ${idea.title}`,
                                          description: idea.description,
                                          priority: 'MEDIUM',
                                        });
                                        setAlertMessage({ type: 'success', text: `Added "${idea.title}" to checklist!` });
                                        loadEventDetails();
                                      } catch (e: unknown) {
                                        if (e instanceof Error) setAlertMessage({ type: 'error', text: e.message });
                                      }
                                    }}
                                  >
                                    <Plus size={14} /> Add to Checklist
                                  </button>
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}

                      {/* Recommended Vendor Categories to Research */}
                      {generatedPlan.recommendedVendorCategories &&
                        generatedPlan.recommendedVendorCategories.length > 0 && (
                          <div className="section-card">
                            <h3 style={{ fontFamily: 'var(--font-heading)', fontSize: '16px', marginBottom: '4px' }}>
                              Recommended Vendor Categories to Research
                            </h3>
                            <p style={{ fontSize: '12px', color: 'var(--text-muted)', marginBottom: '16px' }}>
                              Gatherly helps you vet real vendors with these suggested questions.
                            </p>
                            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '14px' }}>
                              {generatedPlan.recommendedVendorCategories.map((vc, i) => (
                                <div
                                  key={i}
                                  style={{
                                    border: '1px solid var(--border-subtle)',
                                    borderRadius: 'var(--radius-md)',
                                    padding: '14px',
                                    backgroundColor: 'var(--bg-subtle)',
                                  }}
                                >
                                  <strong style={{ fontSize: '13px', color: 'var(--primary-forest)' }}>
                                    {vc.category}
                                  </strong>
                                  <p style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '4px' }}>
                                    {vc.description}
                                  </p>
                                  {vc.tipsForVetting && vc.tipsForVetting.length > 0 && (
                                    <ul style={{ fontSize: '11px', color: 'var(--text-subtle)', marginTop: '8px', paddingLeft: '16px' }}>
                                      {vc.tipsForVetting.map((tip, tIdx) => (
                                        <li key={tIdx} style={{ marginTop: '2px' }}>
                                          {tip}
                                        </li>
                                      ))}
                                    </ul>
                                  )}
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                    </div>
                  )}
                </div>
              )}

              {/* TAB 3: REVISION ASSISTANT */}
              {activeTab === 'revisions' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
                  <div className="section-card">
                    <div className="section-header">
                      <div className="section-title-wrap">
                        <h2>Contextual Plan Revision Assistant</h2>
                        <p>Ask for adjustments without losing your existing manual edits.</p>
                      </div>
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="revision-prompt-input">
                        What would you like to change?
                      </label>
                      <textarea
                        id="revision-prompt-input"
                        className="form-textarea"
                        value={revisionPrompt}
                        onChange={(e) => setRevisionPrompt(e.target.value)}
                        placeholder="e.g. Cut overall budget by 15%, add a live jazz trio, and make the catering menu strictly vegetarian."
                      />
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '16px' }}>
                      <span style={{ fontSize: '12px', color: 'var(--text-subtle)' }}>
                        Shows a non-destructive change preview before any data is updated.
                      </span>
                      <button
                        type="button"
                        className="btn-primary"
                        onClick={handleProposeRevision}
                        disabled={revisionProposing || !revisionPrompt.trim()}
                      >
                        <History size={16} />
                        {revisionProposing ? 'Analyzing Diff...' : 'Preview Proposed Changes'}
                      </button>
                    </div>
                  </div>

                  {/* Version Audit Log */}
                  <div className="section-card">
                    <div className="section-header">
                      <div className="section-title-wrap">
                        <h2>Plan Revision History</h2>
                        <p>Chronological record of applied plan versions.</p>
                      </div>
                    </div>

                    {revisions.length > 0 ? (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                        {revisions.map((rev) => (
                          <div
                            key={rev.id}
                            style={{
                              border: '1px solid var(--border-subtle)',
                              borderRadius: 'var(--radius-md)',
                              padding: '12px 16px',
                              display: 'flex',
                              justifyContent: 'space-between',
                              alignItems: 'center',
                              backgroundColor: 'var(--bg-surface)',
                            }}
                          >
                            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                              <span className="badge badge-neutral" style={{ fontWeight: 800 }}>
                                v{rev.versionNumber}
                              </span>
                              <span style={{ fontSize: '13px', color: 'var(--text-main)' }}>
                                {rev.changeSummary}
                              </span>
                            </div>
                            <span style={{ fontSize: '11px', color: 'var(--text-subtle)', flexShrink: 0 }}>
                              {new Date(rev.createdAt).toLocaleString()}
                            </span>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="empty-state" style={{ padding: '24px' }}>
                        <p>No revisions recorded yet. Revisions are created when you accept an AI plan update.</p>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* TAB 4: BUDGET PLANNER */}
              {activeTab === 'budget' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
                  {budget?.isOverBudget && (
                    <div className="alert-banner warning">
                      <AlertTriangle size={18} style={{ flexShrink: 0 }} />
                      <div>
                        <strong>Over Target Budget:</strong> Total estimated spending is{' '}
                        {selectedEvent.currency} {budget.totalEstimated.toLocaleString()}, exceeding target by{' '}
                        {selectedEvent.currency} {Math.abs(budget.remainingBudget).toLocaleString()}.
                      </div>
                    </div>
                  )}

                  <div className="section-card">
                    <div className="section-header">
                      <div className="section-title-wrap">
                        <h2>Budget Breakdown & Spending Plan</h2>
                        <p>All calculations and constraints are computed deterministically on the server.</p>
                      </div>
                      <button
                        type="button"
                        className="btn-primary"
                        onClick={() => {
                          setEditingBudgetItem(null);
                          setBudgetItemModalOpen(true);
                        }}
                      >
                        <Plus size={16} /> Add Budget Item
                      </button>
                    </div>

                    {/* Category allocation bars */}
                    {budget && budget.categories.length > 0 && (
                      <div style={{ marginBottom: '24px' }}>
                        <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-subtle)', marginBottom: '8px' }}>
                          ALLOCATION BY CATEGORY
                        </div>
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: '12px' }}>
                          {budget.categories.map((cat, i) => (
                            <div
                              key={i}
                              style={{
                                border: '1px solid var(--border-subtle)',
                                borderRadius: 'var(--radius-sm)',
                                padding: '10px 12px',
                                backgroundColor: 'var(--bg-subtle)',
                              }}
                            >
                              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px' }}>
                                <strong>{cat.category}</strong>
                                <span>
                                  {selectedEvent.currency} {cat.estimatedTotal.toLocaleString()}
                                </span>
                              </div>
                              <div className="progress-bar-bg" style={{ height: '4px', margin: '6px 0 2px' }}>
                                <div
                                  className="progress-bar-fill"
                                  style={{
                                    width: `${
                                      selectedEvent.totalBudget > 0
                                        ? Math.min(100, (cat.estimatedTotal / selectedEvent.totalBudget) * 100)
                                        : 0
                                    }%`,
                                  }}
                                />
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Budget items table */}
                    {budget && budget.items.length > 0 ? (
                      <div className="data-table-wrap">
                        <table className="data-table">
                          <thead>
                            <tr>
                              <th>Category</th>
                              <th>Description</th>
                              <th>Estimated Amount</th>
                              <th>Actual Amount</th>
                              <th>Status</th>
                              <th style={{ textAlign: 'right' }}>Actions</th>
                            </tr>
                          </thead>
                          <tbody>
                            {budget.items.map((item) => (
                              <tr key={item.id}>
                                <td>
                                  <strong>{item.category}</strong>
                                </td>
                                <td>{item.description || '—'}</td>
                                <td>
                                  {selectedEvent.currency} {item.estimatedAmount.toLocaleString()}
                                </td>
                                <td>
                                  {item.actualAmount !== null
                                    ? `${selectedEvent.currency} ${item.actualAmount.toLocaleString()}`
                                    : '—'}
                                </td>
                                <td>
                                  {item.isEstimate ? (
                                    <span className="badge badge-estimate">ESTIMATE</span>
                                  ) : (
                                    <span className="badge badge-confirmed">CONFIRMED</span>
                                  )}
                                </td>
                                <td style={{ textAlign: 'right' }}>
                                  <div style={{ display: 'inline-flex', gap: '6px' }}>
                                    <button
                                      type="button"
                                      className="btn-subtle"
                                      onClick={() => {
                                        setEditingBudgetItem(item);
                                        setBudgetItemModalOpen(true);
                                      }}
                                      title="Edit"
                                    >
                                      <Edit2 size={14} />
                                    </button>
                                    <button
                                      type="button"
                                      className="btn-subtle"
                                      style={{ color: 'var(--badge-danger-text)' }}
                                      onClick={() => handleDeleteBudgetItem(item)}
                                      title="Delete"
                                    >
                                      <Trash2 size={14} />
                                    </button>
                                  </div>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    ) : (
                      <div className="empty-state">
                        <Wallet size={28} style={{ color: 'var(--text-subtle)' }} />
                        <h3>No budget items recorded</h3>
                        <p>Generate an initial plan or start adding line items to track costs.</p>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* TAB 5: CHECKLIST & TIMELINE */}
              {activeTab === 'checklist' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
                  <div className="section-card">
                    <div className="section-header" style={{ flexWrap: 'wrap', gap: '12px' }}>
                      <div className="section-title-wrap">
                        <h2>Event Checklist & Milestone Timeline</h2>
                        <p>Sequenced milestones with dependency tracking and overdue date recovery.</p>
                      </div>

                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
                        {/* View Mode Toggle */}
                        <div className="view-mode-switcher">
                          <button
                            type="button"
                            className={`view-mode-btn ${taskViewMode === 'checklist' ? 'active' : ''}`}
                            onClick={() => setTaskViewMode('checklist')}
                          >
                            <LayoutList size={14} />
                            <span>Checklist</span>
                          </button>
                          <button
                            type="button"
                            className={`view-mode-btn ${taskViewMode === 'timeline' ? 'active' : ''}`}
                            onClick={() => setTaskViewMode('timeline')}
                          >
                            <CalendarDays size={14} />
                            <span>Milestone Track</span>
                          </button>
                        </div>

                        <button
                          type="button"
                          className="btn-secondary"
                          onClick={() => setImpactModalOpen(true)}
                          title="Simulate consequences of changing date, guest count, or budget"
                        >
                          <TrendingUp size={15} />
                          <span>Simulate Impact</span>
                        </button>

                        <button
                          type="button"
                          className="btn-primary"
                          onClick={() => {
                            setEditingTask(null);
                            setTaskModalOpen(true);
                          }}
                        >
                          <Plus size={16} /> Add Task
                        </button>
                      </div>
                    </div>

                    {/* CHECKLIST VIEW */}
                    {taskViewMode === 'checklist' && (
                      <>
                        {/* Filter buttons */}
                        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '18px' }}>
                          {(['ALL', 'TODO', 'IN_PROGRESS', 'COMPLETED', 'OVERDUE'] as const).map((filter) => (
                            <button
                              key={filter}
                              type="button"
                              className={`btn-subtle ${taskFilter === filter ? 'active' : ''}`}
                              style={{
                                backgroundColor: taskFilter === filter ? 'var(--primary-forest)' : 'var(--bg-subtle)',
                                color: taskFilter === filter ? '#ffffff' : 'var(--text-muted)',
                                fontWeight: 600,
                              }}
                              onClick={() => setTaskFilter(filter)}
                            >
                              {filter === 'ALL'
                                ? `All (${taskOverview?.totalTasks || 0})`
                                : filter === 'OVERDUE'
                                ? `Overdue (${taskOverview?.overdueTasks || 0})`
                                : filter.replace('_', ' ')}
                            </button>
                          ))}
                        </div>

                        {filteredTasks.length > 0 ? (
                          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                            {filteredTasks.map((task) => (
                              <div
                                key={task.id}
                                className={`checklist-item ${task.status === 'COMPLETED' ? 'completed' : ''}`}
                              >
                                <input
                                  type="checkbox"
                                  className="task-checkbox"
                                  checked={task.status === 'COMPLETED'}
                                  onChange={() => handleToggleTaskStatus(task)}
                                />

                                <div style={{ flex: 1 }}>
                                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                                    <span className="task-title" style={{ fontSize: '13px', fontWeight: 600 }}>
                                      {task.title}
                                    </span>
                                    {task.isOverdue && (
                                      <span className="badge badge-danger">OVERDUE</span>
                                    )}
                                    {task.isBlocked && (
                                      <span
                                        className="badge badge-danger"
                                        title={`Prerequisite task: ${task.dependsOnTaskTitle || 'Uncompleted prerequisite'}`}
                                      >
                                        <Lock size={10} /> Blocked: {task.dependsOnTaskTitle}
                                      </span>
                                    )}
                                    {task.dependsOnTaskId && !task.isBlocked && (
                                      <span
                                        className="badge badge-neutral"
                                        title={`Prerequisite task completed: ${task.dependsOnTaskTitle}`}
                                      >
                                        <Unlock size={10} /> After: {task.dependsOnTaskTitle}
                                      </span>
                                    )}
                                    <span className="badge badge-neutral">{task.priority}</span>
                                    {task.assigneeLabel && (
                                      <span style={{ fontSize: '11px', color: 'var(--text-subtle)' }}>
                                        👤 {task.assigneeLabel}
                                      </span>
                                    )}
                                  </div>
                                  {task.description && (
                                    <p style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '3px' }}>
                                      {task.description}
                                    </p>
                                  )}

                                  {/* Overdue alternative date action */}
                                  {task.isOverdue && task.suggestedAlternativeDate && (
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '6px' }}>
                                      <span style={{ fontSize: '11px', color: 'var(--badge-danger-text)' }}>
                                        Past deadline. Suggested alternative: {task.suggestedAlternativeDate}
                                      </span>
                                      <button
                                        type="button"
                                        className="btn-subtle"
                                        style={{ fontSize: '11px', padding: '2px 6px', color: 'var(--accent-sage)' }}
                                        onClick={() => handleAcceptAlternativeDate(task)}
                                      >
                                        Accept Alternative Date
                                      </button>
                                    </div>
                                  )}
                                </div>

                                {task.dueDate && (
                                  <span style={{ fontSize: '12px', color: 'var(--text-muted)', flexShrink: 0 }}>
                                    Due: {task.dueDate}
                                  </span>
                                )}

                                <div style={{ display: 'inline-flex', gap: '4px' }}>
                                  <button
                                    type="button"
                                    className="btn-subtle"
                                    onClick={() => {
                                      setEditingTask(task);
                                      setTaskModalOpen(true);
                                    }}
                                    title="Edit task"
                                  >
                                    <Edit2 size={14} />
                                  </button>
                                  <button
                                    type="button"
                                    className="btn-subtle"
                                    style={{ color: 'var(--badge-danger-text)' }}
                                    onClick={() => handleDeleteTask(task)}
                                    title="Delete task"
                                  >
                                    <Trash2 size={14} />
                                  </button>
                                </div>
                              </div>
                            ))}
                          </div>
                        ) : (
                          <div className="empty-state">
                            <ListChecks size={28} style={{ color: 'var(--text-subtle)' }} />
                            <h3>No tasks match this filter</h3>
                            <p>Add new tasks to keep your planning organized and on schedule.</p>
                          </div>
                        )}
                      </>
                    )}

                    {/* TIMELINE / MILESTONE TRACK VIEW */}
                    {taskViewMode === 'timeline' && (
                      <div className="timeline-track-container">
                        {/* Overdue Milestones Band */}
                        {(taskOverview?.tasks || []).some((t) => t.isOverdue && t.status !== 'COMPLETED') && (
                          <div className="timeline-band" style={{ borderColor: 'var(--badge-danger-border)' }}>
                            <div className="timeline-band-header" style={{ backgroundColor: 'var(--badge-danger-bg)' }}>
                              <span className="timeline-band-title" style={{ color: 'var(--badge-danger-text)' }}>
                                <AlertTriangle size={14} /> Overdue Milestones — Attention Needed
                              </span>
                              <span className="badge badge-danger">
                                {(taskOverview?.tasks || []).filter((t) => t.isOverdue && t.status !== 'COMPLETED').length} late
                              </span>
                            </div>
                            <div className="timeline-card-list">
                              {(taskOverview?.tasks || [])
                                .filter((t) => t.isOverdue && t.status !== 'COMPLETED')
                                .map((task) => (
                                  <div key={task.id} className="timeline-card">
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: 1 }}>
                                      <input
                                        type="checkbox"
                                        className="task-checkbox"
                                        checked={task.status === 'COMPLETED'}
                                        onChange={() => handleToggleTaskStatus(task)}
                                      />
                                      <div>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                          <strong>{task.title}</strong>
                                          <span className="badge badge-neutral">{task.priority}</span>
                                          {task.isBlocked && (
                                            <span className="badge badge-danger">
                                              <Lock size={10} /> Blocked: {task.dependsOnTaskTitle}
                                            </span>
                                          )}
                                        </div>
                                        {task.suggestedAlternativeDate && (
                                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '4px' }}>
                                            <span style={{ fontSize: '11px', color: 'var(--badge-danger-text)' }}>
                                              Suggested recovery: {task.suggestedAlternativeDate}
                                            </span>
                                            <button
                                              type="button"
                                              className="btn-subtle"
                                              style={{ fontSize: '11px', padding: '2px 6px', color: 'var(--accent-sage)' }}
                                              onClick={() => handleAcceptAlternativeDate(task)}
                                            >
                                              Accept
                                            </button>
                                          </div>
                                        )}
                                      </div>
                                    </div>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                      <span className="timeline-date-pill overdue">
                                        <Clock size={12} /> {task.dueDate} ({task.daysUntilDue ? Math.abs(task.daysUntilDue) : 0}d late)
                                      </span>
                                      <button
                                        type="button"
                                        className="btn-subtle"
                                        onClick={() => {
                                          setEditingTask(task);
                                          setTaskModalOpen(true);
                                        }}
                                      >
                                        <Edit2 size={13} />
                                      </button>
                                    </div>
                                  </div>
                                ))}
                            </div>
                          </div>
                        )}

                        {/* Upcoming / Next 7 Days Band */}
                        <div className="timeline-band">
                          <div className="timeline-band-header">
                            <span className="timeline-band-title" style={{ color: 'var(--primary-forest)' }}>
                              ⚡ This Week / Next 7 Days
                            </span>
                            <span className="badge badge-neutral">
                              {(taskOverview?.tasks || []).filter(
                                (t) => t.dueDate && !t.isOverdue && t.daysUntilDue !== null && t.daysUntilDue >= 0 && t.daysUntilDue <= 7 && t.status !== 'COMPLETED'
                              ).length}
                            </span>
                          </div>
                          <div className="timeline-card-list">
                            {(taskOverview?.tasks || [])
                              .filter((t) => t.dueDate && !t.isOverdue && t.daysUntilDue !== null && t.daysUntilDue >= 0 && t.daysUntilDue <= 7 && t.status !== 'COMPLETED')
                              .map((task) => (
                                <div key={task.id} className="timeline-card">
                                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: 1 }}>
                                    <input
                                      type="checkbox"
                                      className="task-checkbox"
                                      checked={task.status === 'COMPLETED'}
                                      onChange={() => handleToggleTaskStatus(task)}
                                    />
                                    <div>
                                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                        <strong>{task.title}</strong>
                                        <span className="badge badge-neutral">{task.priority}</span>
                                        {task.isBlocked && (
                                          <span className="badge badge-danger">
                                            <Lock size={10} /> Blocked: {task.dependsOnTaskTitle}
                                          </span>
                                        )}
                                      </div>
                                      {task.assigneeLabel && (
                                        <span style={{ fontSize: '11px', color: 'var(--text-subtle)' }}>
                                          👤 {task.assigneeLabel}
                                        </span>
                                      )}
                                    </div>
                                  </div>
                                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                    <span className="timeline-date-pill">
                                      <Calendar size={12} /> {task.dueDate} (in {task.daysUntilDue}d)
                                    </span>
                                    <button
                                      type="button"
                                      className="btn-subtle"
                                      onClick={() => {
                                        setEditingTask(task);
                                        setTaskModalOpen(true);
                                      }}
                                    >
                                      <Edit2 size={13} />
                                    </button>
                                  </div>
                                </div>
                              ))}
                            {!(taskOverview?.tasks || []).some(
                              (t) => t.dueDate && !t.isOverdue && t.daysUntilDue !== null && t.daysUntilDue >= 0 && t.daysUntilDue <= 7 && t.status !== 'COMPLETED'
                            ) && (
                              <div style={{ padding: '14px 16px', fontSize: '12px', color: 'var(--text-subtle)' }}>
                                No tasks scheduled for the next 7 days.
                              </div>
                            )}
                          </div>
                        </div>

                        {/* Next 30 Days Band */}
                        <div className="timeline-band">
                          <div className="timeline-band-header">
                            <span className="timeline-band-title">📅 Next 30 Days</span>
                            <span className="badge badge-neutral">
                              {(taskOverview?.tasks || []).filter(
                                (t) => t.dueDate && !t.isOverdue && t.daysUntilDue !== null && t.daysUntilDue > 7 && t.daysUntilDue <= 30 && t.status !== 'COMPLETED'
                              ).length}
                            </span>
                          </div>
                          <div className="timeline-card-list">
                            {(taskOverview?.tasks || [])
                              .filter((t) => t.dueDate && !t.isOverdue && t.daysUntilDue !== null && t.daysUntilDue > 7 && t.daysUntilDue <= 30 && t.status !== 'COMPLETED')
                              .map((task) => (
                                <div key={task.id} className="timeline-card">
                                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: 1 }}>
                                    <input
                                      type="checkbox"
                                      className="task-checkbox"
                                      checked={task.status === 'COMPLETED'}
                                      onChange={() => handleToggleTaskStatus(task)}
                                    />
                                    <div>
                                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                        <strong>{task.title}</strong>
                                        <span className="badge badge-neutral">{task.priority}</span>
                                        {task.isBlocked && (
                                          <span className="badge badge-danger">
                                            <Lock size={10} /> Blocked: {task.dependsOnTaskTitle}
                                          </span>
                                        )}
                                      </div>
                                      {task.description && (
                                        <p style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '2px' }}>
                                          {task.description}
                                        </p>
                                      )}
                                    </div>
                                  </div>
                                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                    <span className="timeline-date-pill">
                                      <Calendar size={12} /> {task.dueDate} (in {task.daysUntilDue}d)
                                    </span>
                                    <button
                                      type="button"
                                      className="btn-subtle"
                                      onClick={() => {
                                        setEditingTask(task);
                                        setTaskModalOpen(true);
                                      }}
                                    >
                                      <Edit2 size={13} />
                                    </button>
                                  </div>
                                </div>
                              ))}
                          </div>
                        </div>

                        {/* Future Milestones Band (30+ Days) */}
                        <div className="timeline-band">
                          <div className="timeline-band-header">
                            <span className="timeline-band-title">🎯 Later Milestones (30+ Days)</span>
                            <span className="badge badge-neutral">
                              {(taskOverview?.tasks || []).filter(
                                (t) => t.dueDate && !t.isOverdue && t.daysUntilDue !== null && t.daysUntilDue > 30 && t.status !== 'COMPLETED'
                              ).length}
                            </span>
                          </div>
                          <div className="timeline-card-list">
                            {(taskOverview?.tasks || [])
                              .filter((t) => t.dueDate && !t.isOverdue && t.daysUntilDue !== null && t.daysUntilDue > 30 && t.status !== 'COMPLETED')
                              .map((task) => (
                                <div key={task.id} className="timeline-card">
                                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: 1 }}>
                                    <input
                                      type="checkbox"
                                      className="task-checkbox"
                                      checked={task.status === 'COMPLETED'}
                                      onChange={() => handleToggleTaskStatus(task)}
                                    />
                                    <div>
                                      <strong>{task.title}</strong>
                                    </div>
                                  </div>
                                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                    <span className="timeline-date-pill">
                                      <Calendar size={12} /> {task.dueDate} (in {task.daysUntilDue}d)
                                    </span>
                                    <button
                                      type="button"
                                      className="btn-subtle"
                                      onClick={() => {
                                        setEditingTask(task);
                                        setTaskModalOpen(true);
                                      }}
                                    >
                                      <Edit2 size={13} />
                                    </button>
                                  </div>
                                </div>
                              ))}
                          </div>
                        </div>

                        {/* Completed Tasks Band */}
                        {(taskOverview?.tasks || []).some((t) => t.status === 'COMPLETED') && (
                          <div className="timeline-band" style={{ opacity: 0.85 }}>
                            <div className="timeline-band-header">
                              <span className="timeline-band-title" style={{ color: 'var(--accent-sage)' }}>
                                <CheckCircle2 size={14} /> Completed Milestones
                              </span>
                              <span className="badge badge-confirmed">
                                {(taskOverview?.tasks || []).filter((t) => t.status === 'COMPLETED').length} done
                              </span>
                            </div>
                            <div className="timeline-card-list">
                              {(taskOverview?.tasks || [])
                                .filter((t) => t.status === 'COMPLETED')
                                .map((task) => (
                                  <div key={task.id} className="timeline-card completed">
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                                      <input
                                        type="checkbox"
                                        className="task-checkbox"
                                        checked={true}
                                        onChange={() => handleToggleTaskStatus(task)}
                                      />
                                      <span style={{ textDecoration: 'line-through', fontSize: '13px' }}>
                                        {task.title}
                                      </span>
                                    </div>
                                    <span className="badge badge-confirmed">COMPLETED</span>
                                  </div>
                                ))}
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* TAB 6: VENDOR ORGANIZER & QUOTE COMPARISON */}
              {activeTab === 'vendors' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
                  <div className="alert-banner info">
                    <Store size={18} style={{ flexShrink: 0 }} />
                    <div>
                      <strong>Real-world Vendor Tracker:</strong> Gatherly strictly protects your event planning by prohibiting fake vendors, simulated reviews, or fabricated pricing. Every quote is either recorded by you or verified directly with vendors.
                    </div>
                  </div>

                  <div className="section-card">
                    <div className="section-header">
                      <div className="section-title-wrap">
                        <h2>Vendor Directory & Quote Comparison</h2>
                        <p>Track direct vendor quotes, compare competing bids per category, and link booked contracts to your budget.</p>
                      </div>
                      <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                        <div className="view-mode-switcher">
                          <button
                            type="button"
                            className={`view-mode-btn ${vendorViewMode === 'directory' ? 'active' : ''}`}
                            onClick={() => setVendorViewMode('directory')}
                          >
                            <LayoutList size={14} /> Directory View
                          </button>
                          <button
                            type="button"
                            className={`view-mode-btn ${vendorViewMode === 'matrix' ? 'active' : ''}`}
                            onClick={() => setVendorViewMode('matrix')}
                          >
                            <DollarSign size={14} /> Quote Comparison Matrix
                          </button>
                        </div>
                        <button
                          type="button"
                          className="btn-primary"
                          onClick={() => {
                            setEditingVendor(null);
                            setVendorModalOpen(true);
                          }}
                        >
                          <Plus size={16} /> Track New Vendor
                        </button>
                      </div>
                    </div>

                    {vendors.length > 0 ? (
                      vendorViewMode === 'directory' ? (
                        <div className="data-table-wrap">
                          <table className="data-table">
                            <thead>
                              <tr>
                                <th>Category</th>
                                <th>Vendor Name</th>
                                <th>Contact Details</th>
                                <th>Source / Channel</th>
                                <th>Quoted Price</th>
                                <th>Quote Status</th>
                                <th>Booking Status</th>
                                <th>Verified</th>
                                <th style={{ textAlign: 'right' }}>Actions</th>
                              </tr>
                            </thead>
                            <tbody>
                              {vendors.map((vendor) => (
                                <tr key={vendor.id}>
                                  <td>
                                    <strong>{vendor.category}</strong>
                                  </td>
                                  <td>{vendor.name || '—'}</td>
                                  <td>{vendor.contactDetails || '—'}</td>
                                  <td>
                                    <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                                      {vendor.source || 'Manual Entry'}
                                    </span>
                                  </td>
                                  <td>
                                    {vendor.quotedAmount !== null
                                      ? `${selectedEvent.currency} ${vendor.quotedAmount.toLocaleString()}`
                                      : 'Awaiting Quote'}
                                  </td>
                                  <td>
                                    <span
                                      className={`badge ${
                                        vendor.quoteStatus === 'QUOTED'
                                          ? 'badge-confirmed'
                                          : vendor.quoteStatus === 'REJECTED'
                                          ? 'badge-danger'
                                          : 'badge-estimate'
                                      }`}
                                    >
                                      {vendor.quoteStatus}
                                    </span>
                                  </td>
                                  <td>
                                    <span
                                      className={`badge ${
                                        vendor.bookingStatus === 'BOOKED'
                                          ? 'badge-confirmed'
                                          : vendor.bookingStatus === 'CANCELLED'
                                          ? 'badge-danger'
                                          : 'badge-neutral'
                                      }`}
                                    >
                                      {vendor.bookingStatus.replace('_', ' ')}
                                    </span>
                                  </td>
                                  <td>
                                    {vendor.lastVerifiedAt ? (
                                      <span style={{ fontSize: '11px', color: 'var(--badge-confirmed-text)' }}>
                                        {new Date(vendor.lastVerifiedAt).toLocaleDateString()}
                                      </span>
                                    ) : (
                                      <span style={{ fontSize: '11px', color: 'var(--text-subtle)' }}>—</span>
                                    )}
                                  </td>
                                  <td style={{ textAlign: 'right' }}>
                                    <div style={{ display: 'inline-flex', gap: '6px', alignItems: 'center' }}>
                                      {vendor.bookingStatus !== 'BOOKED' && vendor.quotedAmount !== null && (
                                        <button
                                          type="button"
                                          className="btn-secondary"
                                          style={{ padding: '4px 8px', fontSize: '11px', height: '28px' }}
                                          onClick={() => handleBookVendor(vendor)}
                                          title="Book vendor & link quoted price to budget actuals"
                                        >
                                          <CheckCircle2 size={13} /> Book & Link
                                        </button>
                                      )}
                                      <button
                                        type="button"
                                        className="btn-subtle"
                                        onClick={() => {
                                          setEditingVendor(vendor);
                                          setVendorModalOpen(true);
                                        }}
                                        title="Edit vendor"
                                      >
                                        <Edit2 size={14} />
                                      </button>
                                      <button
                                        type="button"
                                        className="btn-subtle"
                                        style={{ color: 'var(--badge-danger-text)' }}
                                        onClick={() => handleDeleteVendor(vendor)}
                                        title="Delete vendor"
                                      >
                                        <Trash2 size={14} />
                                      </button>
                                    </div>
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      ) : (
                        /* QUOTE COMPARISON MATRIX VIEW */
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                          {Array.from(new Set(vendors.map((v) => v.category))).map((categoryName) => {
                            const catVendors = vendors.filter((v) => v.category === categoryName);
                            const categoryBudget = budget?.categories?.find(
                              (c) => c.category.toLowerCase() === categoryName.toLowerCase()
                            );
                            const estimatedBudget = categoryBudget ? categoryBudget.estimatedTotal : null;

                            return (
                              <div key={categoryName} className="matrix-category-card">
                                <div className="matrix-category-header">
                                  <div>
                                    <h3 style={{ fontSize: '15px', fontWeight: 700, margin: 0 }}>
                                      {categoryName}
                                    </h3>
                                    <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                                      {catVendors.length} tracked vendor proposal{catVendors.length > 1 ? 's' : ''}
                                    </span>
                                  </div>
                                  {estimatedBudget !== null && (
                                    <div style={{ textAlign: 'right' }}>
                                      <div style={{ fontSize: '11px', color: 'var(--text-subtle)', textTransform: 'uppercase', fontWeight: 600 }}>
                                        Target Allocation
                                      </div>
                                      <div style={{ fontSize: '14px', fontWeight: 700, color: 'var(--brand-forest)' }}>
                                        {selectedEvent.currency} {estimatedBudget.toLocaleString()}
                                      </div>
                                    </div>
                                  )}
                                </div>

                                <div className="matrix-quotes-grid">
                                  {catVendors.map((vendor) => {
                                    const variance =
                                      vendor.quotedAmount !== null && estimatedBudget !== null
                                        ? vendor.quotedAmount - estimatedBudget
                                        : null;

                                    return (
                                      <div
                                        key={vendor.id}
                                        className={`matrix-vendor-card ${vendor.bookingStatus === 'BOOKED' ? 'booked' : ''}`}
                                      >
                                        <div>
                                          <div className="matrix-vendor-header">
                                            <div>
                                              <h4 style={{ fontSize: '14px', fontWeight: 700, margin: 0 }}>
                                                {vendor.name || 'Unnamed Vendor'}
                                              </h4>
                                              <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                                                {vendor.source || 'Direct Inquiry'}
                                              </span>
                                            </div>
                                            <span
                                              className={`badge ${
                                                vendor.bookingStatus === 'BOOKED'
                                                  ? 'badge-confirmed'
                                                  : vendor.bookingStatus === 'CANCELLED'
                                                  ? 'badge-danger'
                                                  : 'badge-neutral'
                                              }`}
                                            >
                                              {vendor.bookingStatus.replace('_', ' ')}
                                            </span>
                                          </div>

                                          <div style={{ marginTop: '12px' }}>
                                            <div style={{ fontSize: '11px', color: 'var(--text-subtle)', textTransform: 'uppercase', fontWeight: 600 }}>
                                              Quoted Amount
                                            </div>
                                            <div className="matrix-quote-price">
                                              {vendor.quotedAmount !== null
                                                ? `${selectedEvent.currency} ${vendor.quotedAmount.toLocaleString()}`
                                                : 'Awaiting Quote'}
                                            </div>

                                            {variance !== null && (
                                              <div style={{ marginTop: '4px' }}>
                                                {variance <= 0 ? (
                                                  <span className="variance-tag under">
                                                    ✓ {selectedEvent.currency} {Math.abs(variance).toLocaleString()} under budget
                                                  </span>
                                                ) : (
                                                  <span className="variance-tag over">
                                                    ⚠ +{selectedEvent.currency} {variance.toLocaleString()} over estimate
                                                  </span>
                                                )}
                                              </div>
                                            )}
                                          </div>

                                          {vendor.contactDetails && (
                                            <div style={{ fontSize: '12px', color: 'var(--text-main)', marginTop: '8px' }}>
                                              📞 {vendor.contactDetails}
                                            </div>
                                          )}

                                          {vendor.notes && (
                                            <p style={{ fontSize: '11px', color: 'var(--text-subtle)', marginTop: '8px', lineHeight: 1.4 }}>
                                              {vendor.notes}
                                            </p>
                                          )}

                                          {vendor.lastVerifiedAt && (
                                            <div style={{ fontSize: '11px', color: 'var(--badge-confirmed-text)', marginTop: '8px' }}>
                                              Verified quote • {new Date(vendor.lastVerifiedAt).toLocaleDateString()}
                                            </div>
                                          )}
                                        </div>

                                        <div style={{ paddingTop: '12px', borderTop: '1px solid var(--border-subtle)', marginTop: '8px' }}>
                                          {vendor.bookingStatus === 'BOOKED' ? (
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--brand-forest)', fontSize: '12px', fontWeight: 700 }}>
                                              <CheckCircle2 size={16} /> Booked & Locked in Budget
                                            </div>
                                          ) : (
                                            <button
                                              type="button"
                                              className="btn-primary"
                                              style={{ width: '100%', justifyContent: 'center', fontSize: '12px', padding: '6px 12px' }}
                                              onClick={() => handleBookVendor(vendor)}
                                              disabled={vendor.quotedAmount === null}
                                            >
                                              <CheckCircle2 size={14} /> Book & Lock Budget
                                            </button>
                                          )}
                                        </div>
                                      </div>
                                    );
                                  })}
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      )
                    ) : (
                      <div className="empty-state">
                        <Store size={28} style={{ color: 'var(--text-subtle)' }} />
                        <h3>No vendors tracked yet</h3>
                        <p>Keep your quotes, contacts, and booking contracts organized in one place.</p>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </>
          ) : (
            <div className="section-card empty-state">
              <RefreshCw size={24} className="spin" />
              <p>Loading celebration workspace...</p>
            </div>
          )}
        </main>
      </div>

      {/* Global Modals */}
      <AuthModal
        isOpen={authModalOpen}
        onClose={() => setAuthModalOpen(false)}
        onSuccess={(u) => {
          setCurrentUser(u);
          fetchEvents();
        }}
      />

      <NewEventModal
        isOpen={newEventModalOpen}
        eventToEdit={editingEvent}
        onClose={() => {
          setNewEventModalOpen(false);
          setEditingEvent(null);
        }}
        onSuccess={(ev) => {
          setAlertMessage({
            type: 'success',
            text: editingEvent ? `Updated "${ev.name}"!` : `Created "${ev.name}"!`,
          });
          fetchEvents();
          setSelectedEventId(ev.id);
          setSelectedEvent(ev);
        }}
      />

      <RevisionModal
        isOpen={revisionModalOpen}
        proposal={activeProposal}
        currency={selectedEvent?.currency || 'INR'}
        onClose={() => setRevisionModalOpen(false)}
        onSuccess={() => {
          setAlertMessage({ type: 'success', text: 'Plan revision accepted and applied atomically.' });
          loadEventDetails();
        }}
      />

      <AddBudgetItemModal
        isOpen={budgetItemModalOpen}
        eventId={selectedEventId || 0}
        itemToEdit={editingBudgetItem}
        currency={selectedEvent?.currency || 'INR'}
        onClose={() => setBudgetItemModalOpen(false)}
        onSuccess={() => {
          setAlertMessage({ type: 'success', text: 'Budget item saved.' });
          loadEventDetails();
        }}
      />

      <AddTaskModal
        isOpen={taskModalOpen}
        eventId={selectedEventId || 0}
        taskToEdit={editingTask}
        availableTasks={taskOverview?.tasks || []}
        onClose={() => setTaskModalOpen(false)}
        onSuccess={() => {
          setAlertMessage({ type: 'success', text: 'Checklist task saved.' });
          loadEventDetails();
        }}
      />

      <ImpactAnalysisModal
        isOpen={impactModalOpen}
        event={selectedEvent}
        onClose={() => setImpactModalOpen(false)}
        onEventUpdated={() => {
          loadEventDetails();
          fetchEvents();
        }}
      />

      <AddVendorModal
        isOpen={vendorModalOpen}
        eventId={selectedEventId || 0}
        vendorToEdit={editingVendor}
        currency={selectedEvent?.currency || 'INR'}
        onClose={() => setVendorModalOpen(false)}
        onSuccess={() => {
          setAlertMessage({ type: 'success', text: 'Vendor record saved.' });
          loadEventDetails();
        }}
      />

      <ConfirmDialog
        isOpen={confirmDialog.isOpen}
        title={confirmDialog.title}
        message={confirmDialog.message}
        isDanger={true}
        confirmText="Confirm & Delete"
        onConfirm={confirmDialog.onConfirm}
        onCancel={() => setConfirmDialog((prev) => ({ ...prev, isOpen: false }))}
      />
    </div>
  );
}
