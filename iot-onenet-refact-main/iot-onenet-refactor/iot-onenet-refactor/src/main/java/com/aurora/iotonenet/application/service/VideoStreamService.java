package com.aurora.iotonenet.application.service;

import com.aurora.iotonenet.api.dto.VideoStreamInfoDTO;
import com.aurora.iotonenet.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class VideoStreamService {

    private final AppProperties appProperties;
    private final AiDetectionService aiDetectionService;

    public VideoStreamService(AppProperties appProperties, AiDetectionService aiDetectionService) {
        this.appProperties = appProperties;
        this.aiDetectionService = aiDetectionService;
    }

    public List<VideoStreamInfoDTO> listStreams() {
        return List.of(buildDefaultStream());
    }

    public VideoStreamInfoDTO getStream(String streamId) {
        VideoStreamInfoDTO stream = buildDefaultStream();
        return StringUtils.hasText(streamId) && streamId.equals(stream.getStreamId()) ? stream : null;
    }

    private VideoStreamInfoDTO buildDefaultStream() {
        AppProperties.Video config = appProperties.getVideo();
        VideoStreamInfoDTO dto = new VideoStreamInfoDTO();
        dto.setStreamId(config.getDefaultStreamId());
        dto.setDeviceId(config.getDefaultDeviceId());
        dto.setDisplayName(config.getDisplayName());
        dto.setGatewayPageUrl(config.getGatewayPageUrl());
        dto.setPlayerUrl(config.getPlayerUrl());
        dto.setPreferredMode(config.getPreferredMode());
        dto.setFallbackMode(config.getFallbackMode());
        dto.setPublicHost(config.getPublicHost());
        dto.setWebrtcPort(config.getWebrtcPort());
        dto.setAvailable(config.isAvailable() && StringUtils.hasText(config.getPlayerUrl()));
        dto.setAiResultForwarded(aiDetectionService.hasReceivedResult());
        return dto;
    }
}
