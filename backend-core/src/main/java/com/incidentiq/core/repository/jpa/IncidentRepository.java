package com.incidentiq.core.repository.jpa;

import com.incidentiq.core.domain.entity.Incident;
import com.incidentiq.core.domain.enums.IncidentCategory;
import com.incidentiq.core.domain.enums.IncidentPriority;
import com.incidentiq.core.domain.enums.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Page<Incident> findByStatus(IncidentStatus status, Pageable pageable);

    Page<Incident> findByPriority(IncidentPriority priority, Pageable pageable);

    Page<Incident> findByCategory(IncidentCategory category, Pageable pageable);

    Page<Incident> findByReporterId(UUID reporterId, Pageable pageable);

    @Query("""
            SELECT i FROM Incident i
            WHERE (:status IS NULL OR i.status = :status)
              AND (:priority IS NULL OR i.priority = :priority)
              AND (:category IS NULL OR i.category = :category)
            """)
    Page<Incident> findByFilters(
            @Param("status") IncidentStatus status,
            @Param("priority") IncidentPriority priority,
            @Param("category") IncidentCategory category,
            Pageable pageable);

    long countByStatus(IncidentStatus status);

    long countByPriority(IncidentPriority priority);

    @Query("SELECT i FROM Incident i WHERE i.aiProcessed = false AND i.createdAt < :threshold")
    java.util.List<Incident> findUnprocessedBefore(@Param("threshold") java.time.Instant threshold);
}
