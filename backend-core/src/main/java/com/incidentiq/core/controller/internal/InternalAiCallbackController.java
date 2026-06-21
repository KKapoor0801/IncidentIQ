package com.incidentiq.core.controller.internal;

import com.incidentiq.core.dto.request.AiResultCallbackRequest;
import com.incidentiq.core.service.AiCallbackService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/incidents")
public class InternalAiCallbackController {

    private final AiCallbackService aiCallbackService;

    public InternalAiCallbackController(AiCallbackService aiCallbackService) {
        this.aiCallbackService = aiCallbackService;
    }

    @PatchMapping("/{id}/ai-result")
    public ResponseEntity<Void> applyAiResult(@PathVariable UUID id,
                                               @Valid @RequestBody AiResultCallbackRequest request) {
        aiCallbackService.applyAiResult(id, request);
        return ResponseEntity.ok().build();
    }
}
