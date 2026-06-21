package com.incidentiq.core.mapper;

import com.incidentiq.core.domain.entity.Incident;
import com.incidentiq.core.domain.entity.IncidentComment;
import com.incidentiq.core.domain.entity.IncidentHistory;
import com.incidentiq.core.domain.entity.User;
import com.incidentiq.core.dto.response.CommentResponse;
import com.incidentiq.core.dto.response.HistoryResponse;
import com.incidentiq.core.dto.response.IncidentResponse;
import com.incidentiq.core.dto.response.PageResponse;
import com.incidentiq.core.dto.response.UserSummary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class IncidentMapper {

    public IncidentResponse toResponse(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getStatus(),
                incident.getPriority(),
                incident.getCategory(),
                incident.getAiResolutionSuggestion(),
                incident.getAiConfidenceScore(),
                incident.isAiProcessed(),
                toUserSummary(incident.getReporter()),
                incident.getAssignee() != null ? toUserSummary(incident.getAssignee()) : null,
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                incident.getResolvedAt()
        );
    }

    public UserSummary toUserSummary(User user) {
        return new UserSummary(user.getId(), user.getFullName(), user.getEmail());
    }

    public CommentResponse toCommentResponse(IncidentComment comment) {
        return new CommentResponse(
                comment.getId(),
                toUserSummary(comment.getAuthor()),
                comment.getBody(),
                comment.getCreatedAt()
        );
    }

    public HistoryResponse toHistoryResponse(IncidentHistory history) {
        return new HistoryResponse(
                history.getId(),
                history.getFieldChanged(),
                history.getOldValue(),
                history.getNewValue(),
                toUserSummary(history.getChangedBy()),
                history.getChangedAt()
        );
    }

    public <T> PageResponse<T> toPageResponse(Page<?> page, java.util.List<T> content) {
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
