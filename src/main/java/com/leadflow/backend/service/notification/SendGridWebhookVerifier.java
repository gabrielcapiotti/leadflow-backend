package com.leadflow.backend.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class SendGridWebhookVerifier {

    private static final Logger log = LoggerFactory.getLogger(SendGridWebhookVerifier.class);

    @Value("${sendgrid.webhook.verify-signature:true}")
    private boolean verifySignature;

    @Value("${sendgrid.webhook.public-key:}")
    private String publicKey;

    public boolean verify(String signature, String timestamp, String payload) {
        if (!verifySignature) {
            return true;
        }

        if (isBlank(signature) || isBlank(timestamp) || isBlank(payload) || isBlank(publicKey)) {
            log.warn("event=sendgrid_signature_missing_data");
            return false;
        }

        try {
            String signedPayload = timestamp + payload;

            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            byte[] payloadBytes = signedPayload.getBytes(StandardCharsets.UTF_8);
            byte[] decodedKey = Base64.getDecoder().decode(publicKey);

            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
            PublicKey pubKey = keyFactory.generatePublic(keySpec);

            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initVerify(pubKey);
            sig.update(payloadBytes);

            return sig.verify(signatureBytes);
        } catch (Exception ex) {
            log.warn("event=sendgrid_signature_verification_failed message={}", ex.getMessage());
            return false;
        }
    }

    public boolean isVerificationEnabled() {
        return verifySignature;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
