package com.aurora.iotonenet.application.service;

import com.aurora.iotonenet.api.dto.LedOperationRequest;
import com.aurora.iotonenet.api.dto.LedOperationResponse;
import com.aurora.iotonenet.api.dto.OneNetApiResult;
import com.aurora.iotonenet.infrastructure.onenet.OneNetApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class LedOperationApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(LedOperationApplicationService.class);

    private final DeviceStateService deviceStateService;
    private final OneNetApiService oneNetApiService;
    private final OperationRegistryService operationRegistryService;
    private final PlatformLogService platformLogService;

    public LedOperationApplicationService(DeviceStateService deviceStateService,
                                          OneNetApiService oneNetApiService,
                                          OperationRegistryService operationRegistryService,
                                          PlatformLogService platformLogService) {
        this.deviceStateService = deviceStateService;
        this.oneNetApiService = oneNetApiService;
        this.operationRegistryService = operationRegistryService;
        this.platformLogService = platformLogService;
    }

    public LedOperationResponse setLed(LedOperationRequest request) {
        String deviceId = request.getDeviceId();
        String effectiveDeviceName = resolveDeviceName(request);
        Boolean led = request.getLed();

        logger.info("LED command requested: deviceId={}, deviceName={}, led={}", deviceId, effectiveDeviceName, led);
        OneNetApiResult apiResult = oneNetApiService.setLedProperty(effectiveDeviceName, led);

        String requestId;
        String status;
        String message;

        if (apiResult != null && apiResult.isSuccess() && apiResult.getOperationId() != null) {
            requestId = apiResult.getOperationId();
            status = "accepted";
            message = "LED command has been dispatched through OneNET";
            operationRegistryService.registerWith(deviceId, led, requestId);
        } else {
            requestId = operationRegistryService.register(deviceId, led);
            status = "pending";
            message = "OneNET API call failed, command kept as pending. "
                    + (apiResult != null ? apiResult.getBody() : "unknown error");
            logger.warn("OneNET API call failed: httpCode={}, body={}",
                    apiResult != null ? apiResult.getHttpCode() : -1,
                    apiResult != null ? apiResult.getBody() : "null");
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("deviceId", deviceId);
        details.put("deviceName", effectiveDeviceName);
        details.put("led", led);
        details.put("status", status);
        details.put("requestId", requestId);
        details.put("message", message);
        if (apiResult != null) {
            details.put("onenetSuccess", apiResult.isSuccess());
            details.put("httpCode", apiResult.getHttpCode());
            details.put("responseBody", apiResult.getBody());
        }
        platformLogService.record(
                PlatformLogService.TYPE_ONENET_COMMAND,
                deviceId,
                "Issued LED command for device " + (deviceId != null ? deviceId : "unknown") + ", status=" + status,
                details
        );

        return new LedOperationResponse(status, requestId, message);
    }

    private String resolveDeviceName(LedOperationRequest request) {
        String deviceName = request.getDeviceName();
        if (deviceName == null || deviceName.trim().isEmpty()) {
            deviceName = deviceStateService.getCurrentDeviceName();
        }
        if (deviceName == null || deviceName.trim().isEmpty()) {
            deviceName = request.getDeviceId();
        }
        return deviceName;
    }
}
