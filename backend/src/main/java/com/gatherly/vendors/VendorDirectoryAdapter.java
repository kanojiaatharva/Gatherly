package com.gatherly.vendors;

import java.util.List;

public interface VendorDirectoryAdapter {

    boolean isAvailable();

    String getAdapterName();

    List<VendorSearchResult> searchVendors(String category, String location);
}
