package com.gatherly.vendors;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    List<Vendor> findByEventIdOrderByCategoryAscCreatedAtAsc(Long eventId);
    Optional<Vendor> findByIdAndEventId(Long id, Long eventId);
    void deleteByEventId(Long eventId);
}
