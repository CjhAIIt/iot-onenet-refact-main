package com.aurora.iotonenet.infrastructure.pulsar.handler;

import com.aurora.iotonenet.application.service.DeviceIntegrationService;
import org.springframework.stereotype.Component;

@Component
public class DeviceMessageHandler {

    private final DeviceIntegrationService integrationService;

    public DeviceMessageHandler(DeviceIntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    public void handlePropertyReport(String deviceId, String deviceName,
                                     Double temperature, Double humidity,
                                     Double light, Double mq2, Integer error, Boolean led) {
        integrationService.handleDeviceData(deviceId, deviceName, temperature, humidity, light, mq2, error, led);
    }

    public void handleSetReply(String deviceId, String requestId, boolean success, String message) {
        integrationService.handleSetReply(deviceId, requestId, success, message);
    }

    public void handleSetReplyByRequestIdOnly(String requestId, boolean success, String message) {
        integrationService.handleSetReplyByRequestId(requestId, success, message);
    }
}
