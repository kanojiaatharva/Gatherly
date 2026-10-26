package com.gatherly.events;

import com.gatherly.auth.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(length = 255)
    private String location;

    @Column(name = "guest_count")
    private Integer guestCount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "total_budget", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalBudget = BigDecimal.ZERO;

    @Column(name = "style_preferences", columnDefinition = "TEXT")
    private String stylePreferences;

    @Column(name = "dietary_requirements", columnDefinition = "TEXT")
    private String dietaryRequirements;

    @Column(name = "accessibility_needs", columnDefinition = "TEXT")
    private String accessibilityNeeds;

    @Column(columnDefinition = "TEXT")
    private String priorities;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Event() {
    }

    public Event(User user, String name, String eventType, LocalDate eventDate, String location,
                 Integer guestCount, String currency, BigDecimal totalBudget, String stylePreferences,
                 String dietaryRequirements, String accessibilityNeeds, String priorities) {
        this.user = user;
        this.name = name;
        this.eventType = eventType;
        this.eventDate = eventDate;
        this.location = location;
        this.guestCount = guestCount;
        this.currency = currency != null ? currency.toUpperCase() : "INR";
        this.totalBudget = totalBudget != null ? totalBudget : BigDecimal.ZERO;
        this.stylePreferences = stylePreferences;
        this.dietaryRequirements = dietaryRequirements;
        this.accessibilityNeeds = accessibilityNeeds;
        this.priorities = priorities;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(BigDecimal totalBudget) {
        this.totalBudget = totalBudget;
    }

    public String getStylePreferences() {
        return stylePreferences;
    }

    public void setStylePreferences(String stylePreferences) {
        this.stylePreferences = stylePreferences;
    }

    public String getDietaryRequirements() {
        return dietaryRequirements;
    }

    public void setDietaryRequirements(String dietaryRequirements) {
        this.dietaryRequirements = dietaryRequirements;
    }

    public String getAccessibilityNeeds() {
        return accessibilityNeeds;
    }

    public void setAccessibilityNeeds(String accessibilityNeeds) {
        this.accessibilityNeeds = accessibilityNeeds;
    }

    public String getPriorities() {
        return priorities;
    }

    public void setPriorities(String priorities) {
        this.priorities = priorities;
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
