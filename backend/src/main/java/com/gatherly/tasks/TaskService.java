package com.gatherly.tasks;

import com.gatherly.common.BadRequestException;
import com.gatherly.common.ResourceNotFoundException;
import com.gatherly.events.Event;
import com.gatherly.events.OwnershipService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final OwnershipService ownershipService;

    public TaskService(TaskRepository taskRepository, OwnershipService ownershipService) {
        this.taskRepository = taskRepository;
        this.ownershipService = ownershipService;
    }

    @Transactional(readOnly = true)
    public TaskOverviewResponse getTasks(Long eventId, Long userId) {
        Event event = ownershipService.verifyAndGetEvent(eventId, userId);
        List<Task> tasks = taskRepository.findByEventIdOrderByDueDateAscCreatedAtAsc(eventId);

        int total = tasks.size();
        int completed = 0;
        int inProgress = 0;
        int todo = 0;
        int overdue = 0;

        List<TaskResponse> responses = tasks.stream()
                .map(task -> TaskResponse.fromEntity(task, event.getEventDate()))
                .toList();

        for (TaskResponse t : responses) {
            if ("COMPLETED".equalsIgnoreCase(t.status())) {
                completed++;
            } else if ("IN_PROGRESS".equalsIgnoreCase(t.status())) {
                inProgress++;
            } else if ("TODO".equalsIgnoreCase(t.status())) {
                todo++;
            }
            if (t.isOverdue()) {
                overdue++;
            }
        }

        int percentage = total > 0 ? (completed * 100) / total : 0;

        return new TaskOverviewResponse(
                eventId,
                total,
                completed,
                inProgress,
                todo,
                overdue,
                percentage,
                responses
        );
    }

    @Transactional
    public TaskResponse createTask(Long eventId, Long userId, CreateTaskRequest request) {
        Event event = ownershipService.verifyAndGetEvent(eventId, userId);

        Task dependsOn = null;
        if (request.dependsOnTaskId() != null && request.dependsOnTaskId() > 0) {
            dependsOn = taskRepository.findByIdAndEventId(request.dependsOnTaskId(), eventId)
                    .orElseThrow(() -> new BadRequestException("Prerequisite task with ID " + request.dependsOnTaskId() + " does not belong to this event"));
        }

        Task task = new Task(
                event,
                request.title().trim(),
                request.description() != null ? request.description().trim() : null,
                request.dueDate(),
                request.status() != null ? request.status().toUpperCase() : "TODO",
                request.priority() != null ? request.priority().toUpperCase() : "MEDIUM",
                request.assigneeLabel() != null ? request.assigneeLabel().trim() : null,
                dependsOn
        );

        Task saved = taskRepository.save(task);
        return TaskResponse.fromEntity(saved, event.getEventDate());
    }

    @Transactional
    public TaskResponse updateTask(Long eventId, Long taskId, Long userId, UpdateTaskRequest request) {
        Event event = ownershipService.verifyAndGetEvent(eventId, userId);
        Task task = taskRepository.findByIdAndEventId(taskId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + taskId));

        if (request.title() != null && !request.title().trim().isEmpty()) {
            task.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            task.setDescription(request.description().trim());
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }
        if (request.status() != null) {
            task.setStatus(request.status().toUpperCase());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority().toUpperCase());
        }
        if (request.assigneeLabel() != null) {
            task.setAssigneeLabel(request.assigneeLabel().trim());
        }

        if (request.dependsOnTaskId() != null) {
            if (request.dependsOnTaskId() <= 0) {
                task.setDependsOnTask(null);
            } else {
                if (taskId.equals(request.dependsOnTaskId())) {
                    throw new BadRequestException("Task cannot depend on itself");
                }
                Task prerequisite = taskRepository.findByIdAndEventId(request.dependsOnTaskId(), eventId)
                        .orElseThrow(() -> new BadRequestException("Prerequisite task with ID " + request.dependsOnTaskId() + " does not belong to this event"));

                validateNoCircularDependency(task, prerequisite);
                task.setDependsOnTask(prerequisite);
            }
        }

        Task saved = taskRepository.save(task);
        return TaskResponse.fromEntity(saved, event.getEventDate());
    }

    private void validateNoCircularDependency(Task currentTask, Task candidatePrerequisite) {
        Task step = candidatePrerequisite;
        int depth = 0;
        while (step != null && depth < 30) {
            if (currentTask.getId() != null && currentTask.getId().equals(step.getId())) {
                throw new BadRequestException("Circular task dependency detected: task " + currentTask.getId() + " cannot depend on its own descendant");
            }
            step = step.getDependsOnTask();
            depth++;
        }
    }

    @Transactional
    public void deleteTask(Long eventId, Long taskId, Long userId) {
        ownershipService.verifyAndGetEvent(eventId, userId);
        Task task = taskRepository.findByIdAndEventId(taskId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + taskId));

        taskRepository.delete(task);
    }
}
