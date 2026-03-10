package com.aurora.iotonenet.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DeviceIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceIntegrationService.class);

    private final DeviceStateService deviceStateService;
    private final OperationRegistryService operationRegistryService;
    private final PlatformLogService platformLogService;

    public DeviceIntegrationService(DeviceStateService deviceStateService,
                                    OperationRegistryService operationRegistryService,
                                    PlatformLogService platformLogService) {
        this.deviceStateService = deviceStateService;
        this.operationRegistryService = operationRegistryService;
        this.platformLogService = platformLogService;
    }

    public void handleDeviceData(String deviceId, String deviceName,
                                 Double temperature, Double humidity,
                                 Double light, Double mq2, Integer error, Boolean led) {
        long timestamp = System.currentTimeMillis();
        deviceStateService.updateState(deviceId, deviceName, temperature, humidity, light, mq2, error, led, timestamp);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("deviceId", deviceId);
        details.put("deviceName", deviceName);
        details.put("temperature", temperature);
        details.put("humidity", humidity);
        details.put("light", light);
        details.put("mq2", mq2);
        details.put("error", error);
        details.put("led", led);
        details.put("timestampMs", timestamp);
        platformLogService.record(
                PlatformLogService.TYPE_ONENET_UPLINK,
                deviceId,
                "Received OneNET uplink from device " + (deviceId != null ? deviceId : "unknown"),
                details
        );

        logger.info("Device data handled: deviceId={}, deviceName={}, temp={}, hum={}, light={}, mq2={}, error={}, led={}",
                deviceId, deviceName, temperature, humidity, light, mq2, error, led);
    }

    public void handleSetReply(String deviceId, String requestId, boolean success, String message) {
        if (requestId == null) {
            return;
        }

        operationRegistryService.complete(deviceId, requestId, success, message);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("deviceId", deviceId);
        details.put("requestId", requestId);
        details.put("success", success);
        details.put("message", message);
        platformLogService.record(
                PlatformLogService.TYPE_ONENET_SET_REPLY,
                deviceId,
                "Received OneNET set reply for request " + requestId + ", success=" + success,
                details
        );

        logger.info("Set reply handled: deviceId={}, requestId={}, success={}, message={}",
                deviceId, requestId, success, message);
    }

    public void handleSetReplyByRequestId(String requestId, boolean success, String message) {
        if (requestId == null) {
            return;
        }

        operationRegistryService.completeByRequestId(requestId, success, message);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("requestId", requestId);
        details.put("success", success);
        details.put("message", message);
        platformLogService.record(
                PlatformLogService.TYPE_ONENET_SET_REPLY,
                null,
                "Received OneNET set reply for request " + requestId + ", success=" + success,
                details
        );

        logger.info("Set reply handled by requestId: requestId={}, success={}, message={}",
                requestId, success, message);
    }
}
