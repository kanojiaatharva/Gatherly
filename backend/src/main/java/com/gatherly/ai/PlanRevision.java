package com.gatherly.ai;

import com.gatherly.events.Event;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "plan_revisions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_event_version", columnNames = {"event_id", "version_number"})
})
public class PlanRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public PlanRevision() {
    }

    public PlanRevision(Event event, Integer versionNumber, String changeSummary) {
        this.event = event;
        this.versionNumber = versionNumber;
        this.changeSummary = changeSummary;
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

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
