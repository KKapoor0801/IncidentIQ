package com.incidentiq.core.service;

import com.incidentiq.core.domain.entity.Incident;
import com.incidentiq.core.domain.entity.IncidentComment;
import com.incidentiq.core.domain.entity.IncidentHistory;
import com.incidentiq.core.domain.entity.User;
import com.incidentiq.core.domain.enums.IncidentStatus;
import com.incidentiq.core.domain.enums.IncidentStatusTransition;
import com.incidentiq.core.domain.enums.IncidentCategory;
import com.incidentiq.core.domain.enums.IncidentPriority;
import com.incidentiq.core.domain.enums.Role;
import com.incidentiq.core.dto.request.AddCommentRequest;
import com.incidentiq.core.dto.request.CreateIncidentRequest;
import com.incidentiq.core.dto.request.ResolveIncidentRequest;
import com.incidentiq.core.dto.request.UpdateIncidentRequest;
import com.incidentiq.core.dto.response.CommentResponse;
import com.incidentiq.core.dto.response.HistoryResponse;
import com.incidentiq.core.dto.response.IncidentResponse;
import com.incidentiq.core.dto.response.PageResponse;
import com.incidentiq.core.exception.IncidentNotFoundException;
import com.incidentiq.core.exception.ValidationException;
import com.incidentiq.core.kafka.producer.IncidentEventProducer;
import com.incidentiq.core.mapper.IncidentMapper;
import com.incidentiq.core.repository.jpa.IncidentCommentRepository;
import com.incidentiq.core.repository.jpa.IncidentHistoryRepository;
import com.incidentiq.core.repository.jpa.IncidentRepository;
import com.incidentiq.core.repository.jpa.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final IncidentHistoryRepository historyRepository;
    private final IncidentCommentRepository commentRepository;
    private final IncidentMapper mapper;
    private final IncidentCacheService cacheService;
    private final ElasticsearchIndexService esIndexService;
    private final IncidentEventProducer eventProducer;

    public IncidentService(IncidentRepository incidentRepository, UserRepository userRepository,
                           IncidentHistoryRepository historyRepository, IncidentCommentRepository commentRepository,
                           IncidentMapper mapper, IncidentCacheService cacheService,
                           ElasticsearchIndexService esIndexService, IncidentEventProducer eventProducer) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
        this.commentRepository = commentRepository;
        this.mapper = mapper;
        this.cacheService = cacheService;
        this.esIndexService = esIndexService;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public IncidentResponse createIncident(CreateIncidentRequest request, UUID reporterId) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ValidationException("Reporter not found"));

        Incident incident = Incident.builder()
                .title(request.title().strip())
                .description(request.description().strip())
                .reporter(reporter)
                .build();

        incident = incidentRepository.save(incident);
        esIndexService.indexIncident(incident);
        IncidentResponse response = mapper.toResponse(incident);
        cacheService.cacheIncident(response);
        cacheService.bumpListVersion();

        log.info("Incident created: incidentId={}, reporterId={}, title={}",
                incident.getId(), reporterId,
                request.title().length() > 50 ? request.title().substring(0, 50) : request.title());

        publishCreatedSafely(incident);
        return response;
    }

    @Transactional
    public IncidentResponse updateIncident(UUID incidentId, UpdateIncidentRequest request, UUID actingUserId, Role actingRole) {
        Incident incident = findIncidentOrThrow(incidentId);
        User actingUser = userRepository.findById(actingUserId)
                .orElseThrow(() -> new ValidationException("Acting user not found"));

        List<String> changedFields = new ArrayList<>();

        if (request.title() != null) {
            recordHistory(incident, actingUser, "title", incident.getTitle(), request.title());
            incident.setTitle(request.title().strip());
            changedFields.add("title");
        }

        if (request.description() != null) {
            recordHistory(incident, actingUser, "description", incident.getDescription(), request.description());
            incident.setDescription(request.description().strip());
            changedFields.add("description");
        }

        if (request.status() != null && request.status() != incident.getStatus()) {
            validateStatusTransition(incident.getStatus(), request.status(), actingRole);
            recordHistory(incident, actingUser, "status", incident.getStatus().name(), request.status().name());
            incident.setStatus(request.status());
            changedFields.add("status");
        }

        if (request.assigneeId() != null) {
            User assignee = userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new ValidationException("Assignee not found"));
            String oldAssignee = incident.getAssignee() != null ? incident.getAssignee().getId().toString() : null;
            recordHistory(incident, actingUser, "assignee", oldAssignee, assignee.getId().toString());
            incident.setAssignee(assignee);
            changedFields.add("assignee");
        }

        incident = incidentRepository.save(incident);
        esIndexService.indexIncident(incident);
        IncidentResponse response = mapper.toResponse(incident);
        cacheService.onIncidentWrite(incidentId);
        cacheService.cacheIncident(response);

        log.info("Incident updated: incidentId={}, changedFields={}", incidentId, changedFields);

        publishUpdatedSafely(incident, changedFields);
        return response;
    }

    @Transactional
    public IncidentResponse resolveIncident(UUID incidentId, ResolveIncidentRequest request, UUID actingUserId, Role actingRole) {
        Incident incident = findIncidentOrThrow(incidentId);
        User actingUser = userRepository.findById(actingUserId)
                .orElseThrow(() -> new ValidationException("Acting user not found"));

        validateStatusTransition(incident.getStatus(), IncidentStatus.RESOLVED, actingRole);

        recordHistory(incident, actingUser, "status", incident.getStatus().name(), IncidentStatus.RESOLVED.name());
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(Instant.now());

        recordHistory(incident, actingUser, "resolutionNotes", null, request.resolutionNotes());

        incident = incidentRepository.save(incident);
        esIndexService.indexIncident(incident);
        IncidentResponse response = mapper.toResponse(incident);
        cacheService.onIncidentWrite(incidentId);
        cacheService.cacheIncident(response);

        log.info("Incident resolved: incidentId={}, actingUserId={}", incidentId, actingUserId);

        publishUpdatedSafely(incident, List.of("status"));
        return response;
    }

    @Transactional
    public void deleteIncident(UUID incidentId, UUID actingUserId) {
        Incident incident = findIncidentOrThrow(incidentId);
        User actingUser = userRepository.findById(actingUserId)
                .orElseThrow(() -> new ValidationException("Acting user not found"));

        recordHistory(incident, actingUser, "status", incident.getStatus().name(), IncidentStatus.CLOSED.name());
        incident.setStatus(IncidentStatus.CLOSED);
        incidentRepository.save(incident);
        cacheService.onIncidentWrite(incidentId);

        log.info("Incident closed (soft-delete): incidentId={}, actingUserId={}", incidentId, actingUserId);

        publishUpdatedSafely(incident, List.of("status"));
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncident(UUID incidentId) {
        IncidentResponse cached = cacheService.getCachedIncident(incidentId);
        if (cached != null) {
            log.debug("Incident fetched from cache: incidentId={}", incidentId);
            return cached;
        }
        IncidentResponse response = mapper.toResponse(findIncidentOrThrow(incidentId));
        cacheService.cacheIncident(response);
        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<IncidentResponse> listIncidents(IncidentStatus status, IncidentPriority priority,
                                                         IncidentCategory category, Pageable pageable) {
        String statusStr = status != null ? status.name() : null;
        String priorityStr = priority != null ? priority.name() : null;
        String categoryStr = category != null ? category.name() : null;

        PageResponse<IncidentResponse> cached = cacheService.getCachedList(
                statusStr, priorityStr, categoryStr, pageable.getPageNumber(), pageable.getPageSize());
        if (cached != null) {
            log.debug("Incident list fetched from cache");
            return cached;
        }

        Page<Incident> page = incidentRepository.findByFilters(status, priority, category, pageable);
        var content = page.getContent().stream().map(mapper::toResponse).toList();
        PageResponse<IncidentResponse> response = mapper.toPageResponse(page, content);
        cacheService.cacheList(statusStr, priorityStr, categoryStr, pageable.getPageNumber(), pageable.getPageSize(), response);
        return response;
    }

    @Transactional
    public CommentResponse addComment(UUID incidentId, AddCommentRequest request, UUID authorId) {
        Incident incident = findIncidentOrThrow(incidentId);
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ValidationException("Author not found"));

        IncidentComment comment = IncidentComment.builder()
                .incident(incident)
                .author(author)
                .body(request.body())
                .build();

        comment = commentRepository.save(comment);
        log.info("Comment added: incidentId={}, authorId={}", incidentId, authorId);
        return mapper.toCommentResponse(comment);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getComments(UUID incidentId, Pageable pageable) {
        findIncidentOrThrow(incidentId);
        Page<IncidentComment> page = commentRepository.findByIncidentId(incidentId, pageable);
        var content = page.getContent().stream().map(mapper::toCommentResponse).toList();
        return mapper.toPageResponse(page, content);
    }

    @Transactional(readOnly = true)
    public PageResponse<HistoryResponse> getHistory(UUID incidentId, Pageable pageable) {
        findIncidentOrThrow(incidentId);
        Page<IncidentHistory> page = historyRepository.findByIncidentId(incidentId, pageable);
        var content = page.getContent().stream().map(mapper::toHistoryResponse).toList();
        return mapper.toPageResponse(page, content);
    }

    private Incident findIncidentOrThrow(UUID incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));
    }

    private void validateStatusTransition(IncidentStatus from, IncidentStatus to, Role role) {
        if (from == to) return;

        if (!IncidentStatusTransition.isAllowed(from, to)) {
            if (role == Role.ADMIN) {
                return;
            }
            throw new ValidationException(
                    "Invalid status transition from " + from + " to " + to);
        }

        if (IncidentStatusTransition.isBackwardTransition(from, to) && role != Role.ADMIN) {
            throw new ValidationException(
                    "Only ADMIN can transition from " + from + " to " + to);
        }
    }

    private void recordHistory(Incident incident, User changedBy, String field, String oldValue, String newValue) {
        IncidentHistory history = IncidentHistory.builder()
                .incident(incident)
                .fieldChanged(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedBy(changedBy)
                .build();
        historyRepository.save(history);
    }

    private void publishCreatedSafely(Incident incident) {
        try {
            eventProducer.publishCreated(incident);
        } catch (Exception e) {
            log.error("Kafka publish failed for incident.created {}. Incident persisted successfully — publish can be retried.",
                    incident.getId(), e);
        }
    }

    private void publishUpdatedSafely(Incident incident, List<String> changedFields) {
        try {
            eventProducer.publishUpdated(incident, changedFields);
        } catch (Exception e) {
            log.error("Kafka publish failed for incident.updated {}. Incident persisted successfully — publish can be retried.",
                    incident.getId(), e);
        }
    }
}
