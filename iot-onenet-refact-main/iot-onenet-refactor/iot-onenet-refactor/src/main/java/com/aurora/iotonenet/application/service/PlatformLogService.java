package com.aurora.iotonenet.application.service;

import com.aurora.iotonenet.api.dto.PlatformLogEntryDTO;
import com.aurora.iotonenet.api.dto.PlatformLogSummaryDTO;
import com.aurora.iotonenet.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class PlatformLogService {

    public static final String TYPE_ONENET_UPLINK = "ONENET_UPLINK";
    public static final String TYPE_ONENET_COMMAND = "ONENET_COMMAND";
    public static final String TYPE_ONENET_SET_REPLY = "ONENET_SET_REPLY";
    public static final String TYPE_AI_DETECTION = "AI_DETECTION";

    private static final Logger logger = LoggerFactory.getLogger(PlatformLogService.class);
    private static final List<String> SUPPORTED_TYPES = List.of(
            TYPE_ONENET_UPLINK,
            TYPE_ONENET_COMMAND,
            TYPE_ONENET_SET_REPLY,
            TYPE_AI_DETECTION
    );

    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Deque<PlatformLogEntryDTO> recentEntries = new ArrayDeque<>();

    private Path logFilePath;
    private long totalCount;

    public PlatformLogService(ObjectMapper objectMapper, AppProperties appProperties) {
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void init() {
        logFilePath = Path.of(appProperties.getLogs().getFile()).toAbsolutePath().normalize();
        try {
            Path parent = logFilePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (Files.notExists(logFilePath)) {
                Files.createFile(logFilePath);
            }
            loadExistingEntries();
            logger.info("Platform log service ready: file={}, maxInMemory={}, loadedCount={}",
                    logFilePath, appProperties.getLogs().getMaxInMemory(), totalCount);
        } catch (IOException ex) {
            logger.error("Failed to initialize platform log file: {}", logFilePath, ex);
        }
    }

    public PlatformLogEntryDTO record(String type, String deviceId, String summary, Object details) {
        PlatformLogEntryDTO entry = new PlatformLogEntryDTO();
        entry.setEventId(UUID.randomUUID().toString());
        entry.setTimestampMs(System.currentTimeMillis());
        entry.setType(StringUtils.hasText(type) ? type.trim().toUpperCase(Locale.ROOT) : "UNKNOWN");
        entry.setDeviceId(deviceId);
        entry.setSummary(summary);
        entry.setDetails(toJsonNode(details));

        lock.writeLock().lock();
        try {
            appendToFile(entry);
            recentEntries.addFirst(entry);
            while (recentEntries.size() > appProperties.getLogs().getMaxInMemory()) {
                recentEntries.removeLast();
            }
            totalCount++;
        } finally {
            lock.writeLock().unlock();
        }
        return entry;
    }

    public List<PlatformLogEntryDTO> query(String type, String keyword, int limit) {
        int actualLimit = limit <= 0
                ? 100
                : Math.min(limit, Math.max(100, appProperties.getLogs().getMaxInMemory()));
        String expectedType = StringUtils.hasText(type) ? type.trim().toUpperCase(Locale.ROOT) : null;
        String expectedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;

        lock.readLock().lock();
        try {
            List<PlatformLogEntryDTO> result = new ArrayList<>();
            for (PlatformLogEntryDTO entry : recentEntries) {
                if (expectedType != null && !expectedType.equals(entry.getType())) {
                    continue;
                }
                if (expectedKeyword != null && !buildSearchableText(entry).contains(expectedKeyword)) {
                    continue;
                }
                result.add(entry);
                if (result.size() >= actualLimit) {
                    break;
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    public PlatformLogSummaryDTO getSummary() {
        PlatformLogSummaryDTO summary = new PlatformLogSummaryDTO();
        lock.readLock().lock();
        try {
            summary.setCount(totalCount);
            summary.setFile(logFilePath != null ? logFilePath.toString() : null);
            summary.setSupportedTypes(new ArrayList<>(SUPPORTED_TYPES));
            return summary;
        } finally {
            lock.readLock().unlock();
        }
    }

    private JsonNode toJsonNode(Object details) {
        if (details == null) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.valueToTree(details);
    }

    private void appendToFile(PlatformLogEntryDTO entry) {
        if (logFilePath == null) {
            return;
        }
        try {
            String line = objectMapper.writeValueAsString(entry) + System.lineSeparator();
            Files.writeString(logFilePath, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            logger.error("Failed to append platform log entry", ex);
        }
    }

    private void loadExistingEntries() {
        if (logFilePath == null || Files.notExists(logFilePath)) {
            return;
        }

        Deque<PlatformLogEntryDTO> loadedEntries = new ArrayDeque<>();
        long loadedCount = 0L;
        try (BufferedReader reader = Files.newBufferedReader(logFilePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                try {
                    PlatformLogEntryDTO entry = objectMapper.readValue(line, PlatformLogEntryDTO.class);
                    loadedEntries.addLast(entry);
                    while (loadedEntries.size() > appProperties.getLogs().getMaxInMemory()) {
                        loadedEntries.removeFirst();
                    }
                    loadedCount++;
                } catch (Exception ex) {
                    logger.warn("Skip malformed platform log line", ex);
                }
            }
        } catch (IOException ex) {
            logger.error("Failed to load historical platform logs", ex);
        }

        recentEntries.clear();
        while (!loadedEntries.isEmpty()) {
            recentEntries.addFirst(loadedEntries.removeFirst());
        }
        totalCount = loadedCount;
    }

    private String buildSearchableText(PlatformLogEntryDTO entry) {
        StringBuilder builder = new StringBuilder();
        if (entry.getType() != null) {
            builder.append(entry.getType()).append(' ');
        }
        if (entry.getDeviceId() != null) {
            builder.append(entry.getDeviceId()).append(' ');
        }
        if (entry.getSummary() != null) {
            builder.append(entry.getSummary()).append(' ');
        }
        if (entry.getDetails() != null) {
            builder.append(entry.getDetails().toString());
        }
        return builder.toString().toLowerCase(Locale.ROOT);
    }
}
