package com.flashcardapp.helper.security;

import com.flashcardapp.config.OtpProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
public class OtpCodeCipher {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    private final OtpProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpCodeCipher(OtpProperties properties) {
        this.properties = properties;
    }

    public String encrypt(UUID challengeId, String code) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(challengeId.toString().getBytes(StandardCharsets.US_ASCII));
            byte[] encrypted = cipher.doFinal(code.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length)
                            .put(iv)
                            .put(encrypted)
                            .array()
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Không thể bảo vệ nội dung OTP", exception);
        }
    }

    public String decrypt(UUID challengeId, String encryptedCode) {
        try {
            byte[] payload = Base64.getUrlDecoder().decode(encryptedCode);
            if (payload.length <= IV_BYTES) {
                throw new IllegalArgumentException("Nội dung OTP đã mã hóa không hợp lệ");
            }
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(challengeId.toString().getBytes(StandardCharsets.US_ASCII));
            return new String(cipher.doFinal(encrypted), StandardCharsets.US_ASCII);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Không thể đọc nội dung OTP đã bảo vệ", exception);
        }
    }

    private SecretKeySpec key() throws GeneralSecurityException {
        byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(
                ("flashcard:otp-mail:" + properties.requiredHashSecret())
                        .getBytes(StandardCharsets.UTF_8)
        );
        return new SecretKeySpec(keyBytes, "AES");
    }
}
