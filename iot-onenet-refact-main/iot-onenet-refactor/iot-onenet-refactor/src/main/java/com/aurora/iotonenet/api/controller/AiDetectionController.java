package com.aurora.iotonenet.api.controller;

import com.aurora.iotonenet.api.dto.AiDetectionPayload;
import com.aurora.iotonenet.api.dto.AiDetectionSummaryDTO;
import com.aurora.iotonenet.api.dto.ApiResponse;
import com.aurora.iotonenet.application.service.AiDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/edge/ai-detections")
@CrossOrigin(origins = "*")
public class AiDetectionController {

    private final AiDetectionService aiDetectionService;

    public AiDetectionController(AiDetectionService aiDetectionService) {
        this.aiDetectionService = aiDetectionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> accept(@RequestBody AiDetectionPayload payload) {
        aiDetectionService.accept(payload);
        return ResponseEntity.ok(ApiResponse.accepted());
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<AiDetectionSummaryDTO>> latest() {
        return ResponseEntity.ok(ApiResponse.ok(aiDetectionService.getLatest()));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<AiDetectionSummaryDTO>>> history(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(aiDetectionService.listHistory(limit)));
    }
}
