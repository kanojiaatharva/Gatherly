package com.gatherly.tasks;

import com.gatherly.auth.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<TaskOverviewResponse> getTasks(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        TaskOverviewResponse response = taskService.getTasks(eventId, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = taskService.createTask(eventId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long eventId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateTaskRequest request) {
        TaskResponse response = taskService.updateTask(eventId, taskId, principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long eventId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserPrincipal principal) {
        taskService.deleteTask(eventId, taskId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
