package com.gatherly.ai;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanRevisionRepository extends JpaRepository<PlanRevision, Long> {
    List<PlanRevision> findByEventIdOrderByVersionNumberDesc(Long eventId);
    Optional<PlanRevision> findTopByEventIdOrderByVersionNumberDesc(Long eventId);
    void deleteByEventId(Long eventId);
}
