package com.aurora.iotonenet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "onenet")
public class OneNetProperties {

    private String productId;
    private String authorization;
    private String propertySetUrl;
    private String propertyGetUrl;
    private int timeoutMs = 10000;
    private final Pulsar pulsar = new Pulsar();

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getAuthorization() { return authorization; }
    public void setAuthorization(String authorization) { this.authorization = authorization; }
    public String getPropertySetUrl() { return propertySetUrl; }
    public void setPropertySetUrl(String propertySetUrl) { this.propertySetUrl = propertySetUrl; }
    public String getPropertyGetUrl() { return propertyGetUrl; }
    public void setPropertyGetUrl(String propertyGetUrl) { this.propertyGetUrl = propertyGetUrl; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public Pulsar getPulsar() { return pulsar; }

    public static class Pulsar {
        private String brokerUrl = "pulsar+ssl://iot-north-mq.heclouds.com:6651/";
        private String accessId;
        private String secretKey;
        private String subscriptionName;
        private boolean autoOpenDashboard = true;

        public String getBrokerUrl() { return brokerUrl; }
        public void setBrokerUrl(String brokerUrl) { this.brokerUrl = brokerUrl; }
        public String getAccessId() { return accessId; }
        public void setAccessId(String accessId) { this.accessId = accessId; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getSubscriptionName() { return subscriptionName; }
        public void setSubscriptionName(String subscriptionName) { this.subscriptionName = subscriptionName; }
        public boolean isAutoOpenDashboard() { return autoOpenDashboard; }
        public void setAutoOpenDashboard(boolean autoOpenDashboard) { this.autoOpenDashboard = autoOpenDashboard; }
    }
}
