package com.incidentiq.core.controller;

import com.incidentiq.core.domain.enums.IncidentCategory;
import com.incidentiq.core.domain.enums.IncidentPriority;
import com.incidentiq.core.domain.enums.IncidentStatus;
import com.incidentiq.core.domain.enums.Role;
import com.incidentiq.core.dto.request.AddCommentRequest;
import com.incidentiq.core.dto.request.CreateIncidentRequest;
import com.incidentiq.core.dto.request.ResolveIncidentRequest;
import com.incidentiq.core.dto.request.UpdateIncidentRequest;
import com.incidentiq.core.dto.response.CommentResponse;
import com.incidentiq.core.dto.response.HistoryResponse;
import com.incidentiq.core.dto.response.IncidentResponse;
import com.incidentiq.core.dto.response.PageResponse;
import com.incidentiq.core.exception.RateLimitException;
import com.incidentiq.core.service.IncidentService;
import com.incidentiq.core.service.RateLimitService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final RateLimitService rateLimitService;

    public IncidentController(IncidentService incidentService, RateLimitService rateLimitService) {
        this.incidentService = incidentService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ENGINEER', 'ADMIN')")
    public ResponseEntity<IncidentResponse> createIncident(@Valid @RequestBody CreateIncidentRequest request,
                                                            Authentication authentication) {
        UUID reporterId = getUserId(authentication);
        if (rateLimitService.isRateLimited(reporterId, "create-incident")) {
            throw new RateLimitException();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.createIncident(request, reporterId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER', 'ENGINEER', 'ADMIN')")
    public ResponseEntity<IncidentResponse> getIncident(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.getIncident(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ENGINEER', 'ADMIN')")
    public ResponseEntity<IncidentResponse> updateIncident(@PathVariable UUID id,
                                                            @Valid @RequestBody UpdateIncidentRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(incidentService.updateIncident(id, request, getUserId(authentication), getUserRole(authentication)));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ENGINEER', 'ADMIN')")
    public ResponseEntity<IncidentResponse> resolveIncident(@PathVariable UUID id,
                                                             @Valid @RequestBody ResolveIncidentRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(incidentService.resolveIncident(id, request, getUserId(authentication), getUserRole(authentication)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteIncident(@PathVariable UUID id, Authentication authentication) {
        incidentService.deleteIncident(id, getUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER', 'ENGINEER', 'ADMIN')")
    public ResponseEntity<PageResponse<IncidentResponse>> listIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentPriority priority,
            @RequestParam(required = false) IncidentCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(incidentService.listIncidents(status, priority, category, pageable));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('ENGINEER', 'ADMIN')")
    public ResponseEntity<CommentResponse> addComment(@PathVariable UUID id,
                                                       @Valid @RequestBody AddCommentRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.addComment(id, request, getUserId(authentication)));
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('VIEWER', 'ENGINEER', 'ADMIN')")
    public ResponseEntity<PageResponse<CommentResponse>> getComments(@PathVariable UUID id,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.ASC, "createdAt"));
        return ResponseEntity.ok(incidentService.getComments(id, pageable));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('VIEWER', 'ENGINEER', 'ADMIN')")
    public ResponseEntity<PageResponse<HistoryResponse>> getHistory(@PathVariable UUID id,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "changedAt"));
        return ResponseEntity.ok(incidentService.getHistory(id, pageable));
    }

    private UUID getUserId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }

    private Role getUserRole(Authentication authentication) {
        String authority = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_VIEWER");
        return Role.valueOf(authority.replace("ROLE_", ""));
    }
}
