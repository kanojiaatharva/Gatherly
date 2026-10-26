package com.gatherly.vendors;

import com.gatherly.auth.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}/vendors")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping
    public ResponseEntity<List<VendorResponse>> getVendors(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<VendorResponse> vendors = vendorService.getVendors(eventId, principal.getId());
        return ResponseEntity.ok(vendors);
    }

    @PostMapping
    public ResponseEntity<VendorResponse> createVendor(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateVendorRequest request) {
        VendorResponse response = vendorService.createVendor(eventId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{vendorId}")
    public ResponseEntity<VendorResponse> updateVendor(
            @PathVariable Long eventId,
            @PathVariable Long vendorId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateVendorRequest request) {
        VendorResponse response = vendorService.updateVendor(eventId, vendorId, principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{vendorId}/book")
    public ResponseEntity<VendorResponse> bookAndLink(
            @PathVariable Long eventId,
            @PathVariable Long vendorId,
            @RequestParam(required = false) Long budgetItemId,
            @AuthenticationPrincipal UserPrincipal principal) {
        VendorResponse response = vendorService.bookAndLinkToBudget(eventId, vendorId, principal.getId(), budgetItemId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{vendorId}")
    public ResponseEntity<Void> deleteVendor(
            @PathVariable Long eventId,
            @PathVariable Long vendorId,
            @AuthenticationPrincipal UserPrincipal principal) {
        vendorService.deleteVendor(eventId, vendorId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search-adapter-status")
    public ResponseEntity<Map<String, Object>> getAdapterStatus() {
        return ResponseEntity.ok(vendorService.getAdapterStatus());
    }
}
