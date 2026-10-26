package com.gatherly.ai;

import java.util.List;

public record GeneratedVendorCategory(
    String category,
    String description,
    List<String> tipsForVetting
) {}
