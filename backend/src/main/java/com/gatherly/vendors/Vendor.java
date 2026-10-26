package com.gatherly.vendors;

import com.gatherly.events.Event;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "vendors")
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(length = 180)
    private String name;

    @Column(name = "contact_details", length = 255)
    private String contactDetails;

    @Column(name = "quoted_amount", precision = 12, scale = 2)
    private BigDecimal quotedAmount;

    @Column(name = "quote_status", nullable = false, length = 24)
    private String quoteStatus = "UNVERIFIED";

    @Column(name = "booking_status", nullable = false, length = 24)
    private String bookingStatus = "NOT_BOOKED";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(length = 120)
    private String source = "MANUAL_ENTRY";

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Vendor() {
    }

    public Vendor(Event event, String category, String name, String contactDetails,
                  BigDecimal quotedAmount, String quoteStatus, String bookingStatus, String notes) {
        this(event, category, name, contactDetails, quotedAmount, quoteStatus, bookingStatus, notes, "MANUAL_ENTRY", null);
    }

    public Vendor(Event event, String category, String name, String contactDetails,
                  BigDecimal quotedAmount, String quoteStatus, String bookingStatus, String notes,
                  String source, Instant lastVerifiedAt) {
        this.event = event;
        this.category = category;
        this.name = name;
        this.contactDetails = contactDetails;
        this.quotedAmount = quotedAmount;
        this.quoteStatus = quoteStatus != null ? quoteStatus.toUpperCase() : "UNVERIFIED";
        this.bookingStatus = bookingStatus != null ? bookingStatus.toUpperCase() : "NOT_BOOKED";
        this.notes = notes;
        this.source = source != null && !source.isBlank() ? source.trim() : "MANUAL_ENTRY";
        this.lastVerifiedAt = lastVerifiedAt;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(String contactDetails) {
        this.contactDetails = contactDetails;
    }

    public BigDecimal getQuotedAmount() {
        return quotedAmount;
    }

    public void setQuotedAmount(BigDecimal quotedAmount) {
        this.quotedAmount = quotedAmount;
    }

    public String getQuoteStatus() {
        return quoteStatus;
    }

    public void setQuoteStatus(String quoteStatus) {
        this.quoteStatus = quoteStatus;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Instant getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(Instant lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
