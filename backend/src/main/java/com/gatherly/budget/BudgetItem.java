package com.gatherly.budget;

import com.gatherly.events.Event;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "budget_items")
public class BudgetItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(length = 255)
    private String description;

    @Column(name = "estimated_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedAmount = BigDecimal.ZERO;

    @Column(name = "actual_amount", precision = 12, scale = 2)
    private BigDecimal actualAmount;

    @Column(name = "is_estimate", nullable = false)
    private Boolean isEstimate = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public BudgetItem() {
    }

    public BudgetItem(Event event, String category, String description,
                      BigDecimal estimatedAmount, BigDecimal actualAmount, Boolean isEstimate) {
        this.event = event;
        this.category = category;
        this.description = description;
        this.estimatedAmount = estimatedAmount != null ? estimatedAmount : BigDecimal.ZERO;
        this.actualAmount = actualAmount;
        this.isEstimate = isEstimate != null ? isEstimate : true;
        this.createdAt = Instant.now();
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getEstimatedAmount() {
        return estimatedAmount;
    }

    public void setEstimatedAmount(BigDecimal estimatedAmount) {
        this.estimatedAmount = estimatedAmount;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }

    public Boolean getIsEstimate() {
        return isEstimate;
    }

    public void setIsEstimate(Boolean isEstimate) {
        this.isEstimate = isEstimate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
