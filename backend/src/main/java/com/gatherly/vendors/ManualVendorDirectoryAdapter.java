package com.gatherly.vendors;

import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ManualVendorDirectoryAdapter implements VendorDirectoryAdapter {

    private static final Logger log = LoggerFactory.getLogger(ManualVendorDirectoryAdapter.class);

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String getAdapterName() {
        return "Manual & Verified Vendor Directory";
    }

    @Override
    public List<VendorSearchResult> searchVendors(String category, String location) {
        log.info("Direct vendor directory search invoked for category '{}' in '{}'. External commercial directory keys are not configured; enforcing manual vendor entry policy to prevent hallucinated prices.", category, location);
        return Collections.emptyList();
    }
}
