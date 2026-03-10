package com.aurora.iotonenet.api.dto;

import java.util.ArrayList;
import java.util.List;

public class AiDetectionSummaryDTO {

    private String type;
    private String deviceId;
    private String stream;
    private Long timestampMs;
    private Long frameId;
    private Integer imageWidth;
    private Integer imageHeight;
    private Integer detectionCount;
    private Boolean empty;
    private String summary;
    private String overallRiskLevel;
    private List<DetectionItem> items = new ArrayList<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public Long getTimestampMs() {
        return timestampMs;
    }

    public void setTimestampMs(Long timestampMs) {
        this.timestampMs = timestampMs;
    }

    public Long getFrameId() {
        return frameId;
    }

    public void setFrameId(Long frameId) {
        this.frameId = frameId;
    }

    public Integer getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(Integer imageWidth) {
        this.imageWidth = imageWidth;
    }

    public Integer getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    public Integer getDetectionCount() {
        return detectionCount;
    }

    public void setDetectionCount(Integer detectionCount) {
        this.detectionCount = detectionCount;
    }

    public Boolean getEmpty() {
        return empty;
    }

    public void setEmpty(Boolean empty) {
        this.empty = empty;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getOverallRiskLevel() {
        return overallRiskLevel;
    }

    public void setOverallRiskLevel(String overallRiskLevel) {
        this.overallRiskLevel = overallRiskLevel;
    }

    public List<DetectionItem> getItems() {
        return items;
    }

    public void setItems(List<DetectionItem> items) {
        this.items = items;
    }

    public static class DetectionItem {
        private Integer classId;
        private String originalClassName;
        private String displayName;
        private String category;
        private Double confidence;
        private String riskLevel;
        private String advice;
        private List<Double> bbox = new ArrayList<>();
        private List<Double> quad = new ArrayList<>();

        public Integer getClassId() {
            return classId;
        }

        public void setClassId(Integer classId) {
            this.classId = classId;
        }

        public String getOriginalClassName() {
            return originalClassName;
        }

        public void setOriginalClassName(String originalClassName) {
            this.originalClassName = originalClassName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }

        public String getAdvice() {
            return advice;
        }

        public void setAdvice(String advice) {
            this.advice = advice;
        }

        public List<Double> getBbox() {
            return bbox;
        }

        public void setBbox(List<Double> bbox) {
            this.bbox = bbox;
        }

        public List<Double> getQuad() {
            return quad;
        }

        public void setQuad(List<Double> quad) {
            this.quad = quad;
        }
    }
}
