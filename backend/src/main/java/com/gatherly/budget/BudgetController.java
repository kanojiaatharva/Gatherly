package com.gatherly.budget;

import com.gatherly.auth.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping("/budget")
    public ResponseEntity<BudgetOverviewResponse> getBudget(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        BudgetOverviewResponse response = budgetService.getBudget(eventId, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/budget-items")
    public ResponseEntity<BudgetItemResponse> addBudgetItem(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateBudgetItemRequest request) {
        BudgetItemResponse response = budgetService.addBudgetItem(eventId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/budget-items/{itemId}")
    public ResponseEntity<BudgetItemResponse> updateBudgetItem(
            @PathVariable Long eventId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateBudgetItemRequest request) {
        BudgetItemResponse response = budgetService.updateBudgetItem(eventId, itemId, principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/budget-items/{itemId}")
    public ResponseEntity<Void> deleteBudgetItem(
            @PathVariable Long eventId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserPrincipal principal) {
        budgetService.deleteBudgetItem(eventId, itemId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
