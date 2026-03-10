package com.aurora.iotonenet.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class LedOperationRequest {

    @NotBlank(message = "deviceId不能为空")
    private String deviceId;
    private String deviceName;
    @NotNull(message = "led不能为空")
    private Boolean led;

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public Boolean getLed() { return led; }
    public void setLed(Boolean led) { this.led = led; }
}
