package com.gatherly.vendors;

import java.math.BigDecimal;
import java.time.Instant;

public record VendorResponse(
    Long id,
    Long eventId,
    String category,
    String name,
    String contactDetails,
    BigDecimal quotedAmount,
    String quoteStatus,
    String bookingStatus,
    String notes,
    String source,
    Instant lastVerifiedAt,
    Instant createdAt
) {
    public static VendorResponse fromEntity(Vendor vendor) {
        return new VendorResponse(
            vendor.getId(),
            vendor.getEvent().getId(),
            vendor.getCategory(),
            vendor.getName(),
            vendor.getContactDetails(),
            vendor.getQuotedAmount(),
            vendor.getQuoteStatus(),
            vendor.getBookingStatus(),
            vendor.getNotes(),
            vendor.getSource(),
            vendor.getLastVerifiedAt(),
            vendor.getCreatedAt()
        );
    }
}
