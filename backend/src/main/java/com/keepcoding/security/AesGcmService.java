package com.keepcoding.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Criptografia simétrica AES-256-GCM pra dados sensíveis em repouso
 * (tokens OAuth do usuário). Chave de 32 bytes em hex via
 * {@code keepcoding.oauth.encryption-key} (env {@code OAUTH_ENCRYPTION_KEY}).
 *
 * <p>Layout do payload base64: {@code iv (12 bytes) || tag (16) || ciphertext}.
 * IV aleatório por mensagem; tag GCM autentica integridade.</p>
 */
@Slf4j
@Component
public class AesGcmService {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final byte[] key;

    public AesGcmService(@Value("${keepcoding.oauth.encryption-key:}") String hexKey) {
        if (hexKey == null || hexKey.isBlank()) {
            log.warn("[AES] OAUTH_ENCRYPTION_KEY não definida — usando placeholder de DEV. "
                    + "NÃO use isso em produção. Defina uma chave de 32 bytes em hex.");
            this.key = new byte[32]; // zeros — apenas para dev
        } else {
            byte[] decoded = HexFormat.of().parseHex(hexKey.trim());
            if (decoded.length != 32) {
                throw new IllegalStateException(
                        "OAUTH_ENCRYPTION_KEY deve ter 64 caracteres hex (= 32 bytes). "
                                + "Comprimento recebido: " + decoded.length);
            }
            this.key = decoded;
        }
    }

    @PostConstruct
    void warnIfDev() {
        boolean isZero = true;
        for (byte b : key) {
            if (b != 0) {
                isZero = false;
                break;
            }
        }
        if (isZero) {
            log.warn("[AES] Chave de criptografia é toda zeros — modo DEV. "
                    + "Tokens OAuth serão criptografáveis mas sem proteção real.");
        }
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criptografar", e);
        }
    }

    public String decrypt(String b64) {
        if (b64 == null) {
            return null;
        }
        try {
            byte[] in = Base64.getDecoder().decode(b64);
            byte[] iv = new byte[IV_BYTES];
            byte[] ct = new byte[in.length - IV_BYTES];
            System.arraycopy(in, 0, iv, 0, IV_BYTES);
            System.arraycopy(in, IV_BYTES, ct, 0, ct.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao descriptografar", e);
        }
    }
}
