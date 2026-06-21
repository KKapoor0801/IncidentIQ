package com.incidentiq.core.service;

import com.incidentiq.core.domain.enums.IncidentPriority;
import com.incidentiq.core.domain.enums.IncidentStatus;
import com.incidentiq.core.dto.response.DashboardSummary;
import com.incidentiq.core.repository.jpa.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final IncidentRepository incidentRepository;
    private final IncidentCacheService cacheService;

    public DashboardService(IncidentRepository incidentRepository, IncidentCacheService cacheService) {
        this.incidentRepository = incidentRepository;
        this.cacheService = cacheService;
    }

    public DashboardSummary getSummary() {
        Long cachedOpen = cacheService.getCachedDashboardOpenCount();
        long openCount = cachedOpen != null ? cachedOpen : incidentRepository.countByStatus(IncidentStatus.OPEN);
        long inProgressCount = incidentRepository.countByStatus(IncidentStatus.IN_PROGRESS);
        long resolvedCount = incidentRepository.countByStatus(IncidentStatus.RESOLVED);
        long p1Count = incidentRepository.countByPriority(IncidentPriority.P1);
        log.debug("Dashboard summary requested: open={}, inProgress={}, resolved={}, p1={}",
                openCount, inProgressCount, resolvedCount, p1Count);
        return new DashboardSummary(openCount, inProgressCount, resolvedCount, p1Count);
    }

    @Scheduled(fixedRate = 60000)
    public void refreshDashboardCache() {
        long openCount = incidentRepository.countByStatus(IncidentStatus.OPEN);
        cacheService.cacheDashboardOpenCount(openCount);
        log.debug("Dashboard cache refreshed: openCount={}", openCount);
    }
}
