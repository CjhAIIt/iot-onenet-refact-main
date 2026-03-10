package com.aurora.iotonenet.infrastructure.pulsar.auth;

import org.apache.pulsar.client.api.Authentication;
import org.apache.pulsar.client.api.AuthenticationDataProvider;
import org.apache.pulsar.client.api.EncodedAuthenticationParameterSupport;
import org.apache.pulsar.client.api.PulsarClientException;

import java.io.IOException;
import java.util.Map;

public class IoTAuthentication implements Authentication, EncodedAuthenticationParameterSupport {

    private static final String METHOD_NAME = "iot-auth";
    private String iotAccessId;
    private String iotSecretKey;

    public IoTAuthentication() {
    }

    public IoTAuthentication(String iotAccessId, String iotSecretKey) {
        this.iotAccessId = iotAccessId;
        this.iotSecretKey = iotSecretKey;
    }

    @Override
    public String getAuthMethodName() {
        return METHOD_NAME;
    }

    @Override
    public AuthenticationDataProvider getAuthData() throws PulsarClientException {
        return new IoTAuthenticationDataProvider(this.iotAccessId, this.iotSecretKey);
    }

    @Override
    public void configure(String encodedAuthParamString) {
    }

    @Deprecated
    @Override
    public void configure(Map<String, String> authParams) {
    }

    @Override
    public void start() throws PulsarClientException {
    }

    @Override
    public void close() throws IOException {
    }
}
