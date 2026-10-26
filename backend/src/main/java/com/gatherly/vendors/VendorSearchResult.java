package com.gatherly.vendors;

public record VendorSearchResult(
    String name,
    String category,
    String location,
    String contactDetails,
    String websiteUrl,
    String source,
    String notes
) {}
