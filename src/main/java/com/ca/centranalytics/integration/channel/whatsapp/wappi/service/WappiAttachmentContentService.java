package com.ca.centranalytics.integration.channel.whatsapp.wappi.service;

import com.ca.centranalytics.integration.domain.entity.MessageAttachment;
import com.ca.centranalytics.integration.domain.entity.MessageAttachmentContent;
import com.ca.centranalytics.integration.ingestion.dto.InboundAttachment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class WappiAttachmentContentService {

    public Optional<MessageAttachmentContent> buildContent(MessageAttachment attachment, InboundAttachment inboundAttachment) {
        if (!StringUtils.hasText(inboundAttachment.contentBase64())) {
            return Optional.empty();
        }

        byte[] content;
        try {
            content = Base64.getDecoder().decode(inboundAttachment.contentBase64());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid Wappi attachment base64 content", ex);
        }

        return Optional.of(MessageAttachmentContent.builder()
                .attachment(attachment)
                .content(content)
                .fileName(inboundAttachment.fileName())
                .contentSize((long) content.length)
                .contentSha256(hash(content))
                .build());
    }

    private String hash(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }
}
