package com.aurora.iotonenet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Ai ai = new Ai();
    private final Logs logs = new Logs();
    private final Video video = new Video();

    public Ai getAi() {
        return ai;
    }

    public Logs getLogs() {
        return logs;
    }

    public Video getVideo() {
        return video;
    }

    public static class Ai {
        private int maxHistory = 100;

        public int getMaxHistory() {
            return maxHistory;
        }

        public void setMaxHistory(int maxHistory) {
            this.maxHistory = maxHistory;
        }
    }

    public static class Logs {
        private String file = "logs/platform-events.jsonl";
        private int maxInMemory = 500;

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public int getMaxInMemory() {
            return maxInMemory;
        }

        public void setMaxInMemory(int maxInMemory) {
            this.maxInMemory = maxInMemory;
        }
    }

    public static class Video {
        private String defaultStreamId = "k230";
        private String defaultDeviceId = "k230";
        private String displayName = "K230 实时视频流";
        private String gatewayPageUrl = "http://101.35.79.76:1984/";
        private String playerUrl = "http://101.35.79.76:1984/stream.html?src=k230&mode=webrtc,mse";
        private String preferredMode = "webrtc";
        private String fallbackMode = "mse";
        private String publicHost = "101.35.79.76";
        private int webrtcPort = 8555;
        private boolean available = true;

        public String getDefaultStreamId() {
            return defaultStreamId;
        }

        public void setDefaultStreamId(String defaultStreamId) {
            this.defaultStreamId = defaultStreamId;
        }

        public String getDefaultDeviceId() {
            return defaultDeviceId;
        }

        public void setDefaultDeviceId(String defaultDeviceId) {
            this.defaultDeviceId = defaultDeviceId;
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

        public int getWebrtcPort() {
            return webrtcPort;
        }

        public void setWebrtcPort(int webrtcPort) {
            this.webrtcPort = webrtcPort;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }
    }
}
