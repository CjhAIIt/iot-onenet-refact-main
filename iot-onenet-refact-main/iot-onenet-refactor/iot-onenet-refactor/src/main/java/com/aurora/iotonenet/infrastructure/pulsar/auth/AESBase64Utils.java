package com.aurora.iotonenet.infrastructure.pulsar.auth;

import org.apache.pulsar.shade.org.apache.commons.codec.binary.Base64;
import org.apache.pulsar.shade.org.apache.commons.codec.binary.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

public class AESBase64Utils {

    private final String algo = "AES";
    private byte[] keyValue;

    public String decrypt(String encryptedData) throws Exception {
        Key key = generateKey();
        Cipher cipher = Cipher.getInstance(algo);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decodedValue = Base64.decodeBase64(encryptedData);
        byte[] decValue = cipher.doFinal(decodedValue);
        return StringUtils.newStringUtf8(decValue);
    }

    private Key generateKey() {
        return new SecretKeySpec(keyValue, algo);
    }

    public void setKeyValue(byte[] keyValue) {
        this.keyValue = keyValue;
    }

    public static String decrypt(String data, String secretKey) throws Exception {
        AESBase64Utils aes = new AESBase64Utils();
        aes.setKeyValue(secretKey.getBytes());
        return aes.decrypt(data);
    }
}
