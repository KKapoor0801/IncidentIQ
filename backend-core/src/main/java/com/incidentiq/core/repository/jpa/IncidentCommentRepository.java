package com.incidentiq.core.repository.jpa;

import com.incidentiq.core.domain.entity.IncidentComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IncidentCommentRepository extends JpaRepository<IncidentComment, UUID> {

    List<IncidentComment> findByIncidentIdOrderByCreatedAtAsc(UUID incidentId);

    Page<IncidentComment> findByIncidentId(UUID incidentId, Pageable pageable);
}
