import React, { useState } from 'react';
import { api } from '../api';
import { TaskItem } from '../types';
import { X, Plus, Save } from 'lucide-react';

interface AddTaskModalProps {
  isOpen: boolean;
  eventId: number;
  taskToEdit?: TaskItem | null;
  availableTasks?: TaskItem[];
  onClose: () => void;
  onSuccess: () => void;
}

export const AddTaskModal: React.FC<AddTaskModalProps> = ({
  isOpen,
  eventId,
  taskToEdit,
  availableTasks = [],
  onClose,
  onSuccess,
}) => {
  const [title, setTitle] = useState(taskToEdit?.title || '');
  const [description, setDescription] = useState(taskToEdit?.description || '');
  const [dueDate, setDueDate] = useState(taskToEdit?.dueDate || '');
  const [status, setStatus] = useState<TaskItem['status']>(taskToEdit?.status || 'TODO');
  const [priority, setPriority] = useState<TaskItem['priority']>(taskToEdit?.priority || 'MEDIUM');
  const [assigneeLabel, setAssigneeLabel] = useState(taskToEdit?.assigneeLabel || '');
  const [dependsOnTaskId, setDependsOnTaskId] = useState<string>(
    taskToEdit?.dependsOnTaskId ? String(taskToEdit.dependsOnTaskId) : ''
  );
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const depId = dependsOnTaskId ? Number(dependsOnTaskId) : (taskToEdit?.dependsOnTaskId ? -1 : undefined);

      if (taskToEdit) {
        await api.updateTask(eventId, taskToEdit.id, {
          title,
          description: description || null,
          dueDate: dueDate || null,
          status,
          priority,
          assigneeLabel: assigneeLabel || null,
          dependsOnTaskId: depId,
        });
      } else {
        await api.createTask(eventId, {
          title,
          description: description || null,
          dueDate: dueDate || null,
          status,
          priority,
          assigneeLabel: assigneeLabel || null,
          dependsOnTaskId: dependsOnTaskId ? Number(dependsOnTaskId) : undefined,
        });
      }
      onSuccess();
      onClose();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Failed to save task.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true">
      <div className="modal-content" style={{ maxWidth: '480px' }}>
        <div className="modal-header">
          <h3 className="modal-title">{taskToEdit ? 'Edit Task' : 'Add New Task'}</h3>
          <button type="button" className="btn-subtle" onClick={onClose} aria-label="Close dialog">
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && (
              <div className="alert-banner error">
                <span>{error}</span>
              </div>
            )}

            <div className="form-group">
              <label className="form-label" htmlFor="task-title">
                Task Title *
              </label>
              <input
                id="task-title"
                type="text"
                className="form-input"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="e.g. Confirm floral centerpiece design"
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="task-desc">
                Description
              </label>
              <textarea
                id="task-desc"
                className="form-textarea"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Key requirements or notes for this task"
              />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="task-due">
                  Due Date
                </label>
                <input
                  id="task-due"
                  type="date"
                  className="form-input"
                  value={dueDate}
                  onChange={(e) => setDueDate(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="task-assignee">
                  Assignee Label
                </label>
                <input
                  id="task-assignee"
                  type="text"
                  className="form-input"
                  value={assigneeLabel}
                  onChange={(e) => setAssigneeLabel(e.target.value)}
                  placeholder="e.g. Sarah, Couples, Caterer"
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="task-priority">
                  Priority
                </label>
                <select
                  id="task-priority"
                  className="form-select"
                  value={priority}
                  onChange={(e) => setPriority(e.target.value as TaskItem['priority'])}
                >
                  <option value="LOW">Low</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HIGH">High</option>
                  <option value="URGENT">Urgent</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="task-status">
                  Status
                </label>
                <select
                  id="task-status"
                  className="form-select"
                  value={status}
                  onChange={(e) => setStatus(e.target.value as TaskItem['status'])}
                >
                  <option value="TODO">To Do</option>
                  <option value="IN_PROGRESS">In Progress</option>
                  <option value="COMPLETED">Completed</option>
                  <option value="CANCELLED">Cancelled</option>
                </select>
              </div>
            </div>

            <div className="form-group" style={{ marginTop: '14px' }}>
              <label className="form-label" htmlFor="task-dependency">
                Prerequisite Task (Optional Dependency)
              </label>
              <select
                id="task-dependency"
                className="form-select"
                value={dependsOnTaskId}
                onChange={(e) => setDependsOnTaskId(e.target.value)}
              >
                <option value="">None (Independent task)</option>
                {availableTasks
                  .filter((t) => !taskToEdit || t.id !== taskToEdit.id)
                  .map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.title} ({t.status})
                    </option>
                  ))}
              </select>
              <span style={{ fontSize: '11px', color: 'var(--text-subtle)', marginTop: '4px', display: 'block' }}>
                This task will be flagged as blocked until the prerequisite task is completed.
              </span>
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn-secondary" onClick={onClose} disabled={loading}>
              Cancel
            </button>
            <button type="submit" className="btn-primary" disabled={loading}>
              {taskToEdit ? <Save size={16} /> : <Plus size={16} />}
              {loading ? 'Saving...' : taskToEdit ? 'Save Changes' : 'Add Task'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
