package com.aurora.iotonenet.api.controller;

import com.aurora.iotonenet.api.dto.ApiResponse;
import com.aurora.iotonenet.api.dto.VideoStreamInfoDTO;
import com.aurora.iotonenet.application.service.VideoStreamService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/video/streams")
@CrossOrigin(origins = "*")
public class VideoStreamController {

    private final VideoStreamService videoStreamService;

    public VideoStreamController(VideoStreamService videoStreamService) {
        this.videoStreamService = videoStreamService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VideoStreamInfoDTO>>> listStreams() {
        return ResponseEntity.ok(ApiResponse.ok(videoStreamService.listStreams()));
    }

    @GetMapping("/{streamId}")
    public ResponseEntity<ApiResponse<VideoStreamInfoDTO>> getStream(@PathVariable String streamId) {
        VideoStreamInfoDTO stream = videoStreamService.getStream(streamId);
        if (stream == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "stream not found"));
        }
        return ResponseEntity.ok(ApiResponse.ok(stream));
    }
}
