package com.aurora.iotonenet.application.service;

import com.aurora.iotonenet.api.dto.DeviceStateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class DeviceStateService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceStateService.class);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private DeviceStateDTO currentState = new DeviceStateDTO();

    public void updateState(String deviceId, String deviceName,
                            Double temperature, Double humidity,
                            Double light, Double mq2, Integer error,
                            Boolean led, long updatedAt) {
        lock.writeLock().lock();
        try {
            DeviceStateDTO state = new DeviceStateDTO();
            state.setDeviceId(deviceId);
            state.setDeviceName(deviceName);
            state.setTemperature(new DeviceStateDTO.SensorValue(temperature, "°C"));
            state.setHumidity(new DeviceStateDTO.SensorValue(humidity, "%"));
            state.setLight(new DeviceStateDTO.SensorValue(light, "Lux"));
            state.setMq2(new DeviceStateDTO.SensorValue(mq2, "ppm"));
            state.setError(new DeviceStateDTO.ErrorValue(error));
            state.setLed(new DeviceStateDTO.LedValue(led));
            state.setUpdatedAt(updatedAt);
            this.currentState = state;
            logger.info("设备状态已更新: deviceId={}, deviceName={}, temp={}, hum={}, light={}, mq2={}, error={}, led={}",
                    deviceId, deviceName, temperature, humidity, light, mq2, error, led);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public DeviceStateDTO getCurrentState() {
        lock.readLock().lock();
        try {
            return currentState;
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getCurrentDeviceName() {
        lock.readLock().lock();
        try {
            return currentState != null ? currentState.getDeviceName() : null;
        } finally {
            lock.readLock().unlock();
        }
    }
}
