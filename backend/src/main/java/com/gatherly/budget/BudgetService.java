package com.gatherly.budget;

import com.gatherly.common.ResourceNotFoundException;
import com.gatherly.events.Event;
import com.gatherly.events.OwnershipService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {

    private final BudgetItemRepository budgetItemRepository;
    private final OwnershipService ownershipService;

    public BudgetService(
            BudgetItemRepository budgetItemRepository,
            OwnershipService ownershipService) {
        this.budgetItemRepository = budgetItemRepository;
        this.ownershipService = ownershipService;
    }

    @Transactional(readOnly = true)
    public BudgetOverviewResponse getBudget(Long eventId, Long userId) {
        Event event = ownershipService.verifyAndGetEvent(eventId, userId);
        List<BudgetItem> items = budgetItemRepository.findByEventIdOrderByCreatedAtAsc(eventId);

        BigDecimal totalEstimated = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;
        int estimateCount = 0;
        int confirmedCount = 0;

        Map<String, BigDecimal[]> categoryTotals = new LinkedHashMap<>();

        for (BudgetItem item : items) {
            BigDecimal est = item.getEstimatedAmount() != null
                    ? item.getEstimatedAmount().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            totalEstimated = totalEstimated.add(est);

            if (Boolean.TRUE.equals(item.getIsEstimate())) {
                estimateCount++;
            } else {
                confirmedCount++;
            }

            BigDecimal act = BigDecimal.ZERO;
            if (item.getActualAmount() != null) {
                act = item.getActualAmount().setScale(2, RoundingMode.HALF_UP);
                totalActual = totalActual.add(act);
            }

            BigDecimal[] catValues = categoryTotals.computeIfAbsent(item.getCategory(), k -> new BigDecimal[]{
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO // item count
            });
            catValues[0] = catValues[0].add(est);
            catValues[1] = catValues[1].add(act);
            catValues[2] = catValues[2].add(BigDecimal.ONE);
        }

        BigDecimal eventBudget = event.getTotalBudget() != null
                ? event.getTotalBudget().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal remainingBudget = eventBudget.subtract(totalEstimated);
        boolean isOverBudget = totalEstimated.compareTo(eventBudget) > 0;
        BigDecimal variance = totalActual.subtract(totalEstimated);

        List<BudgetCategorySummary> categorySummaries = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> entry : categoryTotals.entrySet()) {
            categorySummaries.add(new BudgetCategorySummary(
                    entry.getKey(),
                    entry.getValue()[0],
                    entry.getValue()[1],
                    entry.getValue()[2].intValue()
            ));
        }

        List<BudgetItemResponse> itemResponses = items.stream()
                .map(BudgetItemResponse::fromEntity)
                .toList();

        return new BudgetOverviewResponse(
                event.getId(),
                event.getCurrency(),
                eventBudget,
                totalEstimated,
                totalActual,
                remainingBudget,
                isOverBudget,
                variance,
                items.size(),
                estimateCount,
                confirmedCount,
                categorySummaries,
                itemResponses
        );
    }

    @Transactional
    public BudgetItemResponse addBudgetItem(Long eventId, Long userId, CreateBudgetItemRequest request) {
        Event event = ownershipService.verifyAndGetEvent(eventId, userId);

        BudgetItem item = new BudgetItem(
                event,
                request.category().trim(),
                request.description() != null ? request.description().trim() : null,
                request.estimatedAmount() != null ? request.estimatedAmount() : BigDecimal.ZERO,
                request.actualAmount(),
                request.isEstimate() != null ? request.isEstimate() : true
        );

        BudgetItem saved = budgetItemRepository.save(item);
        return BudgetItemResponse.fromEntity(saved);
    }

    @Transactional
    public BudgetItemResponse updateBudgetItem(Long eventId, Long itemId, Long userId, UpdateBudgetItemRequest request) {
        ownershipService.verifyAndGetEvent(eventId, userId);
        BudgetItem item = budgetItemRepository.findByIdAndEventId(itemId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget item not found with ID: " + itemId));

        if (request.category() != null && !request.category().trim().isEmpty()) {
            item.setCategory(request.category().trim());
        }
        if (request.description() != null) {
            item.setDescription(request.description().trim());
        }
        if (request.estimatedAmount() != null) {
            item.setEstimatedAmount(request.estimatedAmount());
        }
        if (request.actualAmount() != null) {
            item.setActualAmount(request.actualAmount());
            if (request.isEstimate() == null) {
                item.setIsEstimate(false);
            }
        }
        if (request.isEstimate() != null) {
            item.setIsEstimate(request.isEstimate());
        }

        BudgetItem saved = budgetItemRepository.save(item);
        return BudgetItemResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteBudgetItem(Long eventId, Long itemId, Long userId) {
        ownershipService.verifyAndGetEvent(eventId, userId);
        BudgetItem item = budgetItemRepository.findByIdAndEventId(itemId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget item not found with ID: " + itemId));

        budgetItemRepository.delete(item);
    }
}
