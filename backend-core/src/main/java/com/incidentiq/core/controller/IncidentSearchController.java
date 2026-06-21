package com.incidentiq.core.controller;

import com.incidentiq.core.dto.response.PageResponse;
import com.incidentiq.core.service.IncidentSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class IncidentSearchController {

    private final IncidentSearchService searchService;

    public IncidentSearchController(IncidentSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/incidents/search")
    @PreAuthorize("hasAnyRole('VIEWER', 'ENGINEER', 'ADMIN')")
    @Tag(name = "Incident Search")
    @Operation(summary = "Full-text search across incidents")
    public ResponseEntity<PageResponse<Map<String, Object>>> searchIncidents(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.searchIncidents(query, page, Math.min(size, 100)));
    }

    @GetMapping("/runbooks/search")
    @PreAuthorize("hasAnyRole('VIEWER', 'ENGINEER', 'ADMIN')")
    @Tag(name = "Runbooks")
    @Operation(summary = "Full-text search across runbooks")
    public ResponseEntity<PageResponse<Map<String, Object>>> searchRunbooks(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.searchRunbooks(query, page, Math.min(size, 100)));
    }
}
