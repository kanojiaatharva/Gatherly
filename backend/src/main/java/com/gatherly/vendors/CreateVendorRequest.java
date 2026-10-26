package com.gatherly.vendors;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateVendorRequest(
    @NotBlank(message = "Vendor category is required")
    @Size(max = 80, message = "Category cannot exceed 80 characters")
    String category,

    @Size(max = 180, message = "Name cannot exceed 180 characters")
    String name,

    @Size(max = 255, message = "Contact details cannot exceed 255 characters")
    String contactDetails,

    @DecimalMin(value = "0.00", message = "Quoted amount cannot be negative")
    BigDecimal quotedAmount,

    @Pattern(regexp = "^(?i)(UNVERIFIED|ESTIMATED|QUOTED|REJECTED)$", message = "Quote status must be UNVERIFIED, ESTIMATED, QUOTED, or REJECTED")
    String quoteStatus,

    @Pattern(regexp = "^(?i)(NOT_BOOKED|CONTACTED|SHORTLISTED|BOOKED|CANCELLED)$", message = "Booking status must be NOT_BOOKED, CONTACTED, SHORTLISTED, BOOKED, or CANCELLED")
    String bookingStatus,

    String notes,

    @Size(max = 120, message = "Source description cannot exceed 120 characters")
    String source,

    Instant lastVerifiedAt
) {
    public CreateVendorRequest(String category, String name, String contactDetails,
                               BigDecimal quotedAmount, String quoteStatus, String bookingStatus, String notes) {
        this(category, name, contactDetails, quotedAmount, quoteStatus, bookingStatus, notes, "MANUAL_ENTRY", null);
    }
}
