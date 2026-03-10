package com.aurora.iotonenet.api.dto;

public class VideoStreamInfoDTO {

    private String streamId;
    private String deviceId;
    private String displayName;
    private String gatewayPageUrl;
    private String playerUrl;
    private String preferredMode;
    private String fallbackMode;
    private String publicHost;
    private Integer webrtcPort;
    private Boolean available;
    private Boolean aiResultForwarded;

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getGatewayPageUrl() {
        return gatewayPageUrl;
    }

    public void setGatewayPageUrl(String gatewayPageUrl) {
        this.gatewayPageUrl = gatewayPageUrl;
    }

    public String getPlayerUrl() {
        return playerUrl;
    }

    public void setPlayerUrl(String playerUrl) {
        this.playerUrl = playerUrl;
    }

    public String getPreferredMode() {
        return preferredMode;
    }

    public void setPreferredMode(String preferredMode) {
        this.preferredMode = preferredMode;
    }

    public String getFallbackMode() {
        return fallbackMode;
    }

    public void setFallbackMode(String fallbackMode) {
        this.fallbackMode = fallbackMode;
    }

    public String getPublicHost() {
        return publicHost;
    }

    public void setPublicHost(String publicHost) {
        this.publicHost = publicHost;
    }

    public Integer getWebrtcPort() {
        return webrtcPort;
    }

    public void setWebrtcPort(Integer webrtcPort) {
        this.webrtcPort = webrtcPort;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public Boolean getAiResultForwarded() {
        return aiResultForwarded;
    }

    public void setAiResultForwarded(Boolean aiResultForwarded) {
        this.aiResultForwarded = aiResultForwarded;
    }
}
