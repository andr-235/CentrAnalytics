package com.ca.centranalytics.integration.api.controller;

import com.ca.centranalytics.integration.api.dto.MessageDetailsResponse;
import com.ca.centranalytics.integration.api.dto.MessageResponse;
import com.ca.centranalytics.integration.api.service.MessageQueryService;
import com.ca.centranalytics.integration.domain.entity.Platform;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageQueryService messageQueryService;

    @GetMapping
    public List<MessageResponse> getMessages(
            @RequestParam(required = false) Platform platform,
            @RequestParam(required = false) Long conversationId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String search
    ) {
        return messageQueryService.getMessages(platform, conversationId, authorId, from, to, search);
    }

    @GetMapping("/{id}")
    public MessageDetailsResponse getMessage(@PathVariable Long id) {
        return messageQueryService.getMessage(id);
    }
}
