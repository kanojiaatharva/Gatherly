package com.gatherly.tasks;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByEventIdOrderByDueDateAscCreatedAtAsc(Long eventId);
    Optional<Task> findByIdAndEventId(Long id, Long eventId);
    void deleteByEventId(Long eventId);
}
