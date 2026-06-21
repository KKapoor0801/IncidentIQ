package com.incidentiq.core.repository.jpa;

import com.incidentiq.core.domain.entity.IncidentHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IncidentHistoryRepository extends JpaRepository<IncidentHistory, UUID> {

    List<IncidentHistory> findByIncidentIdOrderByChangedAtDesc(UUID incidentId);

    Page<IncidentHistory> findByIncidentId(UUID incidentId, Pageable pageable);
}
