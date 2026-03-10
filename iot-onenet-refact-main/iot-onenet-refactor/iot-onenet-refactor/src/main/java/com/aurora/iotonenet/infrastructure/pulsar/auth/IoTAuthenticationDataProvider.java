package com.aurora.iotonenet.infrastructure.pulsar.auth;

import org.apache.pulsar.client.api.AuthenticationDataProvider;
import org.apache.pulsar.shade.org.apache.commons.codec.digest.DigestUtils;

import java.util.Map;
import java.util.Set;

public class IoTAuthenticationDataProvider implements AuthenticationDataProvider {

    private final String token;

    public IoTAuthenticationDataProvider() {
        this.token = null;
    }

    public IoTAuthenticationDataProvider(String iotAccessId, String iotSecretKey) {
        this.token = String.format("{\"tenant\":\"%s\",\"password\":\"%s\"}", iotAccessId,
                DigestUtils.sha256Hex(iotAccessId + DigestUtils.sha256Hex(iotSecretKey)).substring(4, 20));
    }

    @Override
    public boolean hasDataForHttp() {
        return false;
    }

    @Override
    public Set<Map.Entry<String, String>> getHttpHeaders() {
        return null;
    }

    @Override
    public boolean hasDataFromCommand() {
        return true;
    }

    @Override
    public String getCommandData() {
        return token;
    }
}
