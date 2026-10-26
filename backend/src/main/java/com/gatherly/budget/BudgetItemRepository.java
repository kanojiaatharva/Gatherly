package com.gatherly.budget;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BudgetItemRepository extends JpaRepository<BudgetItem, Long> {
    List<BudgetItem> findByEventIdOrderByCreatedAtAsc(Long eventId);
    Optional<BudgetItem> findByIdAndEventId(Long id, Long eventId);
    void deleteByEventId(Long eventId);
}
