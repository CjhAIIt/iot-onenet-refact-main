package com.aurora.iotonenet.api.controller;

import com.aurora.iotonenet.api.dto.ApiResponse;
import com.aurora.iotonenet.api.dto.PlatformLogEntryDTO;
import com.aurora.iotonenet.api.dto.PlatformLogSummaryDTO;
import com.aurora.iotonenet.application.service.PlatformLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class PlatformLogController {

    private final PlatformLogService platformLogService;

    public PlatformLogController(PlatformLogService platformLogService) {
        this.platformLogService = platformLogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlatformLogEntryDTO>>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(platformLogService.query(type, keyword, limit)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<PlatformLogSummaryDTO>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(platformLogService.getSummary()));
    }
}
