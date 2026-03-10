package com.aurora.iotonenet.infrastructure.pulsar.parser;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class OriginalMessageParser {

    public ParseResult parse(String originalMsg) {
        JSONObject obj = JSONObject.parseObject(originalMsg);
        if (obj.containsKey("id") && obj.containsKey("code") && !obj.containsKey("subData")) {
            return parseSetReply(obj);
        }
        return parsePropertyReport(obj);
    }

    private ParseResult parseSetReply(JSONObject obj) {
        String replyId = obj.getString("id");
        Integer code = obj.getInteger("code");
        String msg = obj.getString("msg");
        String deviceId = obj.getString("deviceId");
        boolean success = code != null && code == 0;
        return ParseResult.setReply(deviceId, replyId, success, msg);
    }

    private ParseResult parsePropertyReport(JSONObject obj) {
        JSONObject sub = obj.getJSONObject("subData");
        if (sub == null) {
            return ParseResult.invalid("subData为null，无法提取设备信息");
        }

        String deviceId = sub.getString("deviceId");
        String deviceName = sub.getString("deviceName");
        JSONObject params = sub.getJSONObject("params");
        Double temp = null;
        Double hum = null;
        Double light = null;
        Double mq2 = null;
        Integer error = null;
        Boolean led = null;
        long ts = System.currentTimeMillis();

        if (params != null) {
            for (String key : params.keySet()) {
                if (key == null) { continue; }
                JSONObject entry = params.getJSONObject(key);
                if (entry == null) { continue; }

                Long time = entry.getLong("time");
                if (time != null) {
                    ts = Math.max(ts, time);
                }

                Object valueObj = entry.containsKey("value") ? entry.get("value") : entry.get("data");
                String lowerKey = key.toLowerCase();
                if (lowerKey.contains("temp")) {
                    Double parsed = parseDouble(valueObj);
                    if (parsed != null) temp = parsed;
                } else if (lowerKey.contains("hum")) {
                    Double parsed = parseDouble(valueObj);
                    if (parsed != null) hum = parsed;
                } else if (lowerKey.contains("light")) {
                    Double parsed = parseDouble(valueObj);
                    if (parsed != null) light = parsed;
                } else if (lowerKey.contains("mq2")) {
                    Double parsed = parseDouble(valueObj);
                    if (parsed != null) mq2 = parsed;
                } else if (lowerKey.contains("error")) {
                    Double parsed = parseDouble(valueObj);
                    if (parsed != null) error = parsed.intValue();
                } else if (lowerKey.contains("led") || lowerKey.contains("lamp")) {
                    Boolean parsed = parseBoolean(valueObj);
                    if (parsed != null) led = parsed;
                }
            }
        }

        return ParseResult.propertyReport(deviceId, deviceName, temp, hum, light, mq2, error, led, ts);
    }

    private static Double parseDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.doubleValue();
        if (value instanceof String str) {
            str = str.trim();
            if (str.isEmpty()) return null;
            try { return Double.parseDouble(str); } catch (NumberFormatException ignore) { return null; }
        }
        return null;
    }

    private static Boolean parseBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.doubleValue() != 0.0;
        if (value instanceof String str) {
            str = str.trim().toLowerCase();
            if (str.isEmpty()) return null;
            if ("true".equals(str) || "on".equals(str) || "yes".equals(str) || "1".equals(str)) return Boolean.TRUE;
            if ("false".equals(str) || "off".equals(str) || "no".equals(str) || "0".equals(str)) return Boolean.FALSE;
        }
        return null;
    }

    public static class ParseResult {
        private final Type type;
        private final String deviceId;
        private final String deviceName;
        private final String requestId;
        private final boolean success;
        private final String message;
        private final Double temperature;
        private final Double humidity;
        private final Double light;
        private final Double mq2;
        private final Integer error;
        private final Boolean led;
        private final Long timestamp;

        private ParseResult(Type type, String deviceId, String deviceName, String requestId, boolean success, String message,
                            Double temperature, Double humidity, Double light, Double mq2, Integer error, Boolean led, Long timestamp) {
            this.type = type;
            this.deviceId = deviceId;
            this.deviceName = deviceName;
            this.requestId = requestId;
            this.success = success;
            this.message = message;
            this.temperature = temperature;
            this.humidity = humidity;
            this.light = light;
            this.mq2 = mq2;
            this.error = error;
            this.led = led;
            this.timestamp = timestamp;
        }

        public static ParseResult setReply(String deviceId, String requestId, boolean success, String message) {
            return new ParseResult(Type.SET_REPLY, deviceId, null, requestId, success, message, null, null, null, null, null, null, null);
        }

        public static ParseResult propertyReport(String deviceId, String deviceName, Double temperature, Double humidity,
                                                 Double light, Double mq2, Integer error, Boolean led, Long timestamp) {
            return new ParseResult(Type.PROPERTY_REPORT, deviceId, deviceName, null, false, null,
                    temperature, humidity, light, mq2, error, led, timestamp);
        }

        public static ParseResult invalid(String message) {
            return new ParseResult(Type.INVALID, null, null, null, false, message, null, null, null, null, null, null, null);
        }

        public Type getType() { return type; }
        public String getDeviceId() { return deviceId; }
        public String getDeviceName() { return deviceName; }
        public String getRequestId() { return requestId; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Double getTemperature() { return temperature; }
        public Double getHumidity() { return humidity; }
        public Double getLight() { return light; }
        public Double getMq2() { return mq2; }
        public Integer getError() { return error; }
        public Boolean getLed() { return led; }
        public Long getTimestamp() { return timestamp; }
    }

    public enum Type {
        SET_REPLY, PROPERTY_REPORT, INVALID
    }
}
