package com.gatherly.vendors;

import com.gatherly.budget.BudgetItem;
import com.gatherly.budget.BudgetItemRepository;
import com.gatherly.common.ResourceNotFoundException;
import com.gatherly.events.Event;
import com.gatherly.events.OwnershipService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final OwnershipService ownershipService;
    private final VendorDirectoryAdapter vendorDirectoryAdapter;

    public VendorService(
            VendorRepository vendorRepository,
            BudgetItemRepository budgetItemRepository,
            OwnershipService ownershipService,
            VendorDirectoryAdapter vendorDirectoryAdapter) {
        this.vendorRepository = vendorRepository;
        this.budgetItemRepository = budgetItemRepository;
        this.ownershipService = ownershipService;
        this.vendorDirectoryAdapter = vendorDirectoryAdapter;
    }

    @Transactional(readOnly = true)
    public List<VendorResponse> getVendors(Long eventId, Long userId) {
        ownershipService.verifyAndGetEvent(eventId, userId);
        return vendorRepository.findByEventIdOrderByCategoryAscCreatedAtAsc(eventId)
                .stream()
                .map(VendorResponse::fromEntity)
                .toList();
    }

    @Transactional
    public VendorResponse createVendor(Long eventId, Long userId, CreateVendorRequest request) {
        Event event = ownershipService.verifyAndGetEvent(eventId, userId);

        String quoteStatus = request.quoteStatus() != null ? request.quoteStatus().toUpperCase() : "UNVERIFIED";
        Instant verifiedAt = request.lastVerifiedAt();
        if ("QUOTED".equals(quoteStatus) && verifiedAt == null) {
            verifiedAt = Instant.now();
        }

        Vendor vendor = new Vendor(
                event,
                request.category().trim(),
                request.name() != null ? request.name().trim() : null,
                request.contactDetails() != null ? request.contactDetails().trim() : null,
                request.quotedAmount(),
                quoteStatus,
                request.bookingStatus() != null ? request.bookingStatus().toUpperCase() : "NOT_BOOKED",
                request.notes() != null ? request.notes().trim() : null,
                request.source() != null && !request.source().isBlank() ? request.source().trim() : "MANUAL_ENTRY",
                verifiedAt
        );

        Vendor saved = vendorRepository.save(vendor);
        return VendorResponse.fromEntity(saved);
    }

    @Transactional
    public VendorResponse updateVendor(Long eventId, Long vendorId, Long userId, UpdateVendorRequest request) {
        ownershipService.verifyAndGetEvent(eventId, userId);
        Vendor vendor = vendorRepository.findByIdAndEventId(vendorId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor record not found with ID: " + vendorId));

        if (request.category() != null && !request.category().trim().isEmpty()) {
            vendor.setCategory(request.category().trim());
        }
        if (request.name() != null) {
            vendor.setName(request.name().trim());
        }
        if (request.contactDetails() != null) {
            vendor.setContactDetails(request.contactDetails().trim());
        }
        if (request.quotedAmount() != null) {
            vendor.setQuotedAmount(request.quotedAmount());
        }
        if (request.quoteStatus() != null) {
            String newQuoteStatus = request.quoteStatus().toUpperCase();
            vendor.setQuoteStatus(newQuoteStatus);
            if ("QUOTED".equals(newQuoteStatus) && vendor.getLastVerifiedAt() == null) {
                vendor.setLastVerifiedAt(Instant.now());
            }
        }
        if (request.bookingStatus() != null) {
            vendor.setBookingStatus(request.bookingStatus().toUpperCase());
        }
        if (request.notes() != null) {
            vendor.setNotes(request.notes().trim());
        }
        if (request.source() != null && !request.source().isBlank()) {
            vendor.setSource(request.source().trim());
        }
        if (request.lastVerifiedAt() != null) {
            vendor.setLastVerifiedAt(request.lastVerifiedAt());
        }

        Vendor saved = vendorRepository.save(vendor);
        return VendorResponse.fromEntity(saved);
    }

    @Transactional
    public VendorResponse bookAndLinkToBudget(Long eventId, Long vendorId, Long userId, Long budgetItemId) {
        ownershipService.verifyAndGetEvent(eventId, userId);
        Vendor vendor = vendorRepository.findByIdAndEventId(vendorId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor record not found with ID: " + vendorId));

        vendor.setBookingStatus("BOOKED");
        if (vendor.getQuotedAmount() != null) {
            vendor.setQuoteStatus("QUOTED");
            if (vendor.getLastVerifiedAt() == null) {
                vendor.setLastVerifiedAt(Instant.now());
            }
        }

        // Link with budget item if budgetItemId is provided, or match by category
        if (budgetItemId != null) {
            budgetItemRepository.findByIdAndEventId(budgetItemId, eventId).ifPresent(item -> {
                if (vendor.getQuotedAmount() != null) {
                    item.setActualAmount(vendor.getQuotedAmount());
                    item.setIsEstimate(false);
                }
                budgetItemRepository.save(item);
            });
        } else if (vendor.getCategory() != null) {
            List<BudgetItem> items = budgetItemRepository.findByEventIdOrderByCreatedAtAsc(eventId);
            for (BudgetItem item : items) {
                if (item.getCategory().equalsIgnoreCase(vendor.getCategory())) {
                    if (vendor.getQuotedAmount() != null) {
                        item.setActualAmount(vendor.getQuotedAmount());
                        item.setIsEstimate(false);
                    }
                    budgetItemRepository.save(item);
                    break;
                }
            }
        }

        Vendor saved = vendorRepository.save(vendor);
        return VendorResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteVendor(Long eventId, Long vendorId, Long userId) {
        ownershipService.verifyAndGetEvent(eventId, userId);
        Vendor vendor = vendorRepository.findByIdAndEventId(vendorId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor record not found with ID: " + vendorId));

        vendorRepository.delete(vendor);
    }

    public Map<String, Object> getAdapterStatus() {
        return Map.of(
                "adapterAvailable", vendorDirectoryAdapter.isAvailable(),
                "adapterName", vendorDirectoryAdapter.getAdapterName(),
                "policy", "To prevent hallucinated pricing and non-existent vendors, automated web scraping is disabled. Users must record direct vendor quotes or configure verified commercial API keys."
        );
    }
}
