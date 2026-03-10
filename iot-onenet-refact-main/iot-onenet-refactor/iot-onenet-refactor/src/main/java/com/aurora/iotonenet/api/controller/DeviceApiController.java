package com.aurora.iotonenet.api.controller;

import com.aurora.iotonenet.api.dto.DeviceStateDTO;
import com.aurora.iotonenet.api.dto.LedOperationRequest;
import com.aurora.iotonenet.api.dto.LedOperationResponse;
import com.aurora.iotonenet.application.service.DeviceStateService;
import com.aurora.iotonenet.application.service.LedOperationApplicationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DeviceApiController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceApiController.class);
    private final DeviceStateService deviceStateService;
    private final LedOperationApplicationService ledOperationApplicationService;

    public DeviceApiController(DeviceStateService deviceStateService,
                               LedOperationApplicationService ledOperationApplicationService) {
        this.deviceStateService = deviceStateService;
        this.ledOperationApplicationService = ledOperationApplicationService;
    }

    @GetMapping("/status")
    public ResponseEntity<DeviceStateDTO> getDeviceStatus() {
        DeviceStateDTO state = deviceStateService.getCurrentState();
        logger.info("API请求 /api/status，返回设备状态: deviceId={}, deviceName={}, temp={}, hum={}, light={}, mq2={}, error={}",
                state.getDeviceId(), state.getDeviceName(),
                state.getTemperature() != null ? state.getTemperature().getValue() : null,
                state.getHumidity() != null ? state.getHumidity().getValue() : null,
                state.getLight() != null ? state.getLight().getValue() : null,
                state.getMq2() != null ? state.getMq2().getValue() : null,
                state.getError() != null ? state.getError().getValue() : null);
        return ResponseEntity.ok(state);
    }

    @PostMapping("/ops/led")
    public ResponseEntity<LedOperationResponse> setLed(@Valid @RequestBody LedOperationRequest request) {
        LedOperationResponse response = ledOperationApplicationService.setLed(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
}
