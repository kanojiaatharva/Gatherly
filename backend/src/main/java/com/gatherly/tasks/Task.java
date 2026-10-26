package com.gatherly.tasks;

import com.gatherly.events.Event;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false, length = 24)
    private String status = "TODO";

    @Column(nullable = false, length = 16)
    private String priority = "MEDIUM";

    @Column(name = "assignee_label", length = 120)
    private String assigneeLabel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depends_on_task_id")
    private Task dependsOnTask;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Task() {
    }

    public Task(Event event, String title, String description, LocalDate dueDate,
                String status, String priority, String assigneeLabel) {
        this(event, title, description, dueDate, status, priority, assigneeLabel, null);
    }

    public Task(Event event, String title, String description, LocalDate dueDate,
                String status, String priority, String assigneeLabel, Task dependsOnTask) {
        this.event = event;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.status = status != null ? status.toUpperCase() : "TODO";
        this.priority = priority != null ? priority.toUpperCase() : "MEDIUM";
        this.assigneeLabel = assigneeLabel;
        this.dependsOnTask = dependsOnTask;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getAssigneeLabel() {
        return assigneeLabel;
    }

    public void setAssigneeLabel(String assigneeLabel) {
        this.assigneeLabel = assigneeLabel;
    }

    public Task getDependsOnTask() {
        return dependsOnTask;
    }

    public void setDependsOnTask(Task dependsOnTask) {
        this.dependsOnTask = dependsOnTask;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
