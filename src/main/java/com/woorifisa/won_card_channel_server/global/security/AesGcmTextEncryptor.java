package com.woorifisa.won_card_channel_server.global.security;

import com.woorifisa.won_card_channel_server.global.config.SecurityProperties;
import jakarta.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class AesGcmTextEncryptor implements TextEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int AES_KEY_LENGTH_BYTES = 32;
    private static final String ENCRYPTED_PREFIX = "enc:";

    private final SecurityProperties securityProperties;
    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKey secretKey;

    public AesGcmTextEncryptor(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @PostConstruct
    void initialize() {
        try {
            String secret = securityProperties.getCryptoSecret();
            byte[] decodedKey = Base64.getDecoder().decode(secret);
            if (decodedKey.length != AES_KEY_LENGTH_BYTES) {
                throw new IllegalStateException("app.security.crypto-secret must be a Base64-encoded 32-byte key");
            }
            this.secretKey = new SecretKeySpec(decodedKey, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize text encryptor", e);
        }
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt text", e);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            String normalizedCipherText = cipherText.startsWith(ENCRYPTED_PREFIX)
                    ? cipherText.substring(ENCRYPTED_PREFIX.length())
                    : cipherText;
            byte[] decoded = Base64.getDecoder().decode(normalizedCipherText);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt text", e);
        }
    }

    @Override
    public String decryptIfPossible(String value) {
        if (value == null) {
            return null;
        }
        if (!value.startsWith(ENCRYPTED_PREFIX)) {
            return value;
        }
        return decrypt(value);
    }
}
