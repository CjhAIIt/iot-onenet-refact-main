package com.aurora.iotonenet.application.service;

import com.aurora.iotonenet.api.dto.AiDetectionPayload;
import com.aurora.iotonenet.api.dto.AiDetectionSummaryDTO;
import com.aurora.iotonenet.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class AiDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(AiDetectionService.class);

    private static final DetectionProfile GENERIC_PEST = new DetectionProfile(
            "虫害目标", "虫害", "中", "建议结合现场图像复核叶片和茎部，优先采取局部防治措施。"
    );
    private static final DetectionProfile GENERIC_DISEASE = new DetectionProfile(
            "病害目标", "病害", "中", "建议隔离异常植株并复核叶面、叶背和根部状况。"
    );
    private static final DetectionProfile HEALTHY = new DetectionProfile(
            "健康", "健康", "健康", "当前未见明显病虫害特征，继续保持常规巡检。"
    );
    private static final DetectionProfile UNKNOWN = new DetectionProfile(
            "未分类目标", "未分类", "未知", "边缘端标签未命中已知病虫害字典，建议人工复核。"
    );

    private static final List<DetectionRule> DETECTION_RULES = List.of(
            new DetectionRule("蚜虫", "虫害", "高", "建议优先检查嫩叶背面，可结合黄板和定点喷施处理。",
                    "aphid", "蚜虫"),
            new DetectionRule("红蜘蛛", "虫害", "高", "建议提高通风并复核叶背斑点，必要时进行杀螨处理。",
                    "spider_mite", "red_spider", "红蜘蛛"),
            new DetectionRule("蓟马", "虫害", "高", "建议检查花器和嫩梢，及时进行蓝板诱捕和局部防治。",
                    "thrips", "蓟马"),
            new DetectionRule("粉虱", "虫害", "中", "建议复核叶背群聚情况，配合黄板和局部药剂干预。",
                    "whitefly", "粉虱"),
            new DetectionRule("毛虫", "虫害", "中", "建议检查叶缘和嫩梢啃食痕迹，及时清除虫体。",
                    "caterpillar", "worm", "毛虫"),
            new DetectionRule("白粉病", "病害", "高", "建议降低湿度并清理受感染叶片，避免继续扩散。",
                    "powdery_mildew", "白粉病"),
            new DetectionRule("霜霉病", "病害", "高", "建议加强通风，降低叶面结露时间并复核病斑扩散。",
                    "downy_mildew", "霜霉病"),
            new DetectionRule("叶斑病", "病害", "中", "建议检查叶片病斑边缘，优先处理受害区域。",
                    "leaf_spot", "spot", "叶斑病"),
            new DetectionRule("锈病", "病害", "中", "建议检查叶片背部孢子堆，及时清理受害叶片。",
                    "rust", "锈病"),
            new DetectionRule("疫病/枯萎病", "病害", "极高", "建议立即隔离疑似植株并复核根系、茎基部状态。",
                    "blight", "wilt", "疫病", "枯萎病"),
            new DetectionRule("炭疽病", "病害", "高", "建议复核叶片和果实凹陷病斑，避免高湿环境持续。",
                    "anthracnose", "炭疽病"),
            new DetectionRule("健康", "健康", "健康", "当前未见明显病虫害特征，继续保持常规巡检。",
                    "healthy", "normal", "健康")
    );

    private final PlatformLogService platformLogService;
    private final AppProperties appProperties;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Deque<AiDetectionSummaryDTO> history = new ArrayDeque<>();

    private AiDetectionSummaryDTO latest;

    public AiDetectionService(PlatformLogService platformLogService, AppProperties appProperties) {
        this.platformLogService = platformLogService;
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void init() {
        logger.info("AI detection service ready: maxHistory={}", appProperties.getAi().getMaxHistory());
    }

    public AiDetectionSummaryDTO accept(AiDetectionPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("AI detection payload must not be null");
        }

        AiDetectionSummaryDTO summary = buildSummary(payload);
        lock.writeLock().lock();
        try {
            latest = summary;
            history.addFirst(summary);
            while (history.size() > appProperties.getAi().getMaxHistory()) {
                history.removeLast();
            }
        } finally {
            lock.writeLock().unlock();
        }

        platformLogService.record(
                PlatformLogService.TYPE_AI_DETECTION,
                summary.getDeviceId(),
                summary.getSummary(),
                payload
        );

        logger.info("Accepted AI detection: deviceId={}, stream={}, frameId={}, count={}, overallRisk={}",
                summary.getDeviceId(), summary.getStream(), summary.getFrameId(),
                summary.getDetectionCount(), summary.getOverallRiskLevel());
        return summary;
    }

    public AiDetectionSummaryDTO getLatest() {
        lock.readLock().lock();
        try {
            return latest;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<AiDetectionSummaryDTO> listHistory(int limit) {
        int actualLimit = limit <= 0
                ? 20
                : Math.min(limit, appProperties.getAi().getMaxHistory());
        lock.readLock().lock();
        try {
            List<AiDetectionSummaryDTO> result = new ArrayList<>();
            int index = 0;
            for (AiDetectionSummaryDTO item : history) {
                result.add(item);
                index++;
                if (index >= actualLimit) {
                    break;
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean hasReceivedResult() {
        lock.readLock().lock();
        try {
            return latest != null;
        } finally {
            lock.readLock().unlock();
        }
    }

    private AiDetectionSummaryDTO buildSummary(AiDetectionPayload payload) {
        AiDetectionSummaryDTO summary = new AiDetectionSummaryDTO();
        summary.setType(StringUtils.hasText(payload.getType()) ? payload.getType() : "AI_DETECTIONS");
        summary.setDeviceId(StringUtils.hasText(payload.getDeviceId()) ? payload.getDeviceId() : "k230");
        summary.setStream(StringUtils.hasText(payload.getStream()) ? payload.getStream() : "k230");
        summary.setTimestampMs(payload.getTimestampMs() != null ? payload.getTimestampMs() : System.currentTimeMillis());
        summary.setFrameId(payload.getFrameId());

        if (payload.getImage() != null) {
            summary.setImageWidth(payload.getImage().getWidth());
            summary.setImageHeight(payload.getImage().getHeight());
        }

        List<AiDetectionPayload.Detection> detections = payload.getDetections() != null
                ? payload.getDetections()
                : Collections.emptyList();
        List<AiDetectionSummaryDTO.DetectionItem> items = new ArrayList<>();
        for (AiDetectionPayload.Detection detection : detections) {
            items.add(toDetectionItem(detection));
        }

        summary.setItems(items);
        summary.setDetectionCount(items.size());
        summary.setEmpty(items.isEmpty());
        summary.setOverallRiskLevel(resolveOverallRisk(items));
        summary.setSummary(buildSummaryText(items, summary.getOverallRiskLevel()));
        return summary;
    }

    private AiDetectionSummaryDTO.DetectionItem toDetectionItem(AiDetectionPayload.Detection detection) {
        DetectionProfile profile = resolveProfile(detection.getClassName());
        AiDetectionSummaryDTO.DetectionItem item = new AiDetectionSummaryDTO.DetectionItem();
        item.setClassId(detection.getClassId());
        item.setOriginalClassName(detection.getClassName());
        item.setDisplayName(profile.displayName);
        item.setCategory(profile.category);
        item.setConfidence(detection.getConfidence());
        item.setRiskLevel(resolveRiskLevel(profile, detection.getConfidence()));
        item.setAdvice(profile.advice);
        item.setBbox(detection.getBbox() != null ? detection.getBbox() : Collections.emptyList());
        item.setQuad(detection.getQuad() != null ? detection.getQuad() : Collections.emptyList());
        return item;
    }

    private DetectionProfile resolveProfile(String className) {
        String normalized = normalize(className);
        for (DetectionRule rule : DETECTION_RULES) {
            if (rule.matches(normalized)) {
                return new DetectionProfile(rule.displayName, rule.category, rule.baseRiskLevel, rule.advice);
            }
        }
        if (containsAny(normalized, "aphid", "mite", "thrip", "whitefly", "worm", "bug", "虫", "蚜", "螨")) {
            return GENERIC_PEST;
        }
        if (containsAny(normalized, "mildew", "blight", "rust", "spot", "disease", "rot", "病", "霉", "斑")) {
            return GENERIC_DISEASE;
        }
        if (containsAny(normalized, "healthy", "normal", "健康")) {
            return HEALTHY;
        }
        return UNKNOWN;
    }

    private String resolveRiskLevel(DetectionProfile profile, Double confidence) {
        if ("健康".equals(profile.baseRiskLevel)) {
            return "健康";
        }
        String confidenceRisk = riskFromConfidence(confidence);
        return maxRisk(profile.baseRiskLevel, confidenceRisk);
    }

    private String riskFromConfidence(Double confidence) {
        if (confidence == null) {
            return "未知";
        }
        if (confidence >= 0.93d) {
            return "高";
        }
        if (confidence >= 0.78d) {
            return "中";
        }
        return "低";
    }

    private String resolveOverallRisk(List<AiDetectionSummaryDTO.DetectionItem> items) {
        if (items.isEmpty()) {
            return "健康";
        }
        String result = "低";
        for (AiDetectionSummaryDTO.DetectionItem item : items) {
            result = maxRisk(result, item.getRiskLevel());
        }
        return result;
    }

    private String buildSummaryText(List<AiDetectionSummaryDTO.DetectionItem> items, String overallRiskLevel) {
        if (items.isEmpty()) {
            return "当前未检测到病虫害目标";
        }

        Set<String> names = new LinkedHashSet<>();
        for (AiDetectionSummaryDTO.DetectionItem item : items) {
            names.add(item.getDisplayName());
            if (names.size() >= 3) {
                break;
            }
        }
        return "检测到 " + items.size() + " 个病虫害目标，最高风险：" + overallRiskLevel
                + "，主要目标：" + String.join("、", names);
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String className) {
        if (className == null) {
            return "";
        }
        return className.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private String maxRisk(String left, String right) {
        return rankOf(left) >= rankOf(right) ? left : right;
    }

    private int rankOf(String risk) {
        if (risk == null) {
            return 0;
        }
        return switch (risk) {
            case "低" -> 1;
            case "中" -> 2;
            case "高" -> 3;
            case "极高" -> 4;
            default -> 0;
        };
    }

    private static final class DetectionProfile {
        private final String displayName;
        private final String category;
        private final String baseRiskLevel;
        private final String advice;

        private DetectionProfile(String displayName, String category, String baseRiskLevel, String advice) {
            this.displayName = displayName;
            this.category = category;
            this.baseRiskLevel = baseRiskLevel;
            this.advice = advice;
        }
    }

    private static final class DetectionRule {
        private final String displayName;
        private final String category;
        private final String baseRiskLevel;
        private final String advice;
        private final List<String> keywords;

        private DetectionRule(String displayName, String category, String baseRiskLevel, String advice, String... keywords) {
            this.displayName = displayName;
            this.category = category;
            this.baseRiskLevel = baseRiskLevel;
            this.advice = advice;
            this.keywords = List.of(keywords);
        }

        private boolean matches(String normalizedName) {
            for (String keyword : keywords) {
                if (normalizedName.equals(keyword) || normalizedName.contains(keyword)) {
                    return true;
                }
            }
            return false;
        }
    }
}
