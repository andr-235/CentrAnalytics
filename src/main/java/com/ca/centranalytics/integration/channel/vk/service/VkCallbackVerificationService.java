package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.api.exception.WebhookVerificationException;
import com.ca.centranalytics.integration.channel.vk.config.VkProperties;
import com.ca.centranalytics.integration.channel.vk.dto.VkCallbackRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class VkCallbackVerificationService {

    private final VkProperties vkProperties;

    public boolean isConfirmation(VkCallbackRequest request) {
        return "confirmation".equals(request.type());
    }

    public String confirmationCode() {
        return vkProperties.confirmationCode();
    }

    public void verify(VkCallbackRequest request) {
        if (request.groupId() == null || request.groupId() != vkProperties.groupId()) {
            throw new WebhookVerificationException("VK group id is invalid");
        }
        if (StringUtils.hasText(vkProperties.secret()) && !vkProperties.secret().equals(request.secret())) {
            throw new WebhookVerificationException("VK secret is invalid");
        }
    }
}
