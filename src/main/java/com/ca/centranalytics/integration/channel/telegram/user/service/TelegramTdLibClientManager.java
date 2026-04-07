package com.ca.centranalytics.integration.channel.telegram.user.service;

import com.ca.centranalytics.integration.channel.telegram.user.config.TelegramUserProperties;
import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSession;
import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSessionState;
import com.ca.centranalytics.integration.channel.telegram.user.exception.TelegramUserModeDisabledException;
import com.ca.centranalytics.integration.ingestion.service.IntegrationIngestionService;
import it.tdlight.client.APIToken;
import it.tdlight.client.AuthenticationSupplier;
import it.tdlight.client.ClientInteraction;
import it.tdlight.client.InputParameter;
import it.tdlight.client.ParameterInfoPasswordHint;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.client.SimpleTelegramClientBuilder;
import it.tdlight.client.SimpleTelegramClientFactory;
import it.tdlight.client.TDLibSettings;
import it.tdlight.jni.TdApi;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramTdLibClientManager {

    private final TelegramUserProperties properties;
    private final TelegramUserSessionService sessionService;
    private final TelegramUserInboundEventMapper inboundEventMapper;
    private final IntegrationIngestionService integrationIngestionService;

    private final Map<Long, ClientRuntime> runtimes = new ConcurrentHashMap<>();
    private final SimpleTelegramClientFactory clientFactory = new SimpleTelegramClientFactory();

    public synchronized void startOrResume(TelegramUserSession session) {
        ensureEnabled();
        if (runtimes.containsKey(session.getId())) {
            return;
        }

        try {
            Files.createDirectories(Path.of(session.getTdlibDatabasePath()));
            Files.createDirectories(Path.of(session.getTdlibFilesPath()));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize TDLib directories", ex);
        }

        ClientRuntime runtime = new ClientRuntime();
        runtimes.put(session.getId(), runtime);

        SimpleTelegramClientBuilder builder = clientFactory.builder(buildSettings(session));
        builder.setClientInteraction(buildInteraction(session.getId(), runtime));
        builder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, update -> handleAuthorizationState(session.getId(), update));
        builder.addUpdateHandler(TdApi.UpdateNewMessage.class, update -> handleNewMessage(session.getId(), update.message, "message_new"));
        builder.addUpdateHandler(TdApi.UpdateMessageEdited.class, update -> handleEditedMessage(session.getId(), update));

        runtime.client = builder.build(AuthenticationSupplier.user(session.getPhoneNumber()));
        configureProxy(runtime.client);
        if (!session.isAuthorized()) {
            sessionService.updateState(session.getId(), TelegramUserSessionState.WAIT_CODE, null);
        }
    }

    public synchronized void submitCode(Long sessionId, String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("code is required");
        }
        ClientRuntime runtime = getRuntime(sessionId);
        runtime.ensureCodeFuture().complete(code.trim());
    }

    public synchronized void submitPassword(Long sessionId, String password) {
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("password is required");
        }
        ClientRuntime runtime = getRuntime(sessionId);
        runtime.ensurePasswordFuture().complete(password);
    }

    @PreDestroy
    void closeAll() {
        runtimes.values().forEach(ClientRuntime::closeSilently);
        runtimes.clear();
        clientFactory.close();
    }

    private TDLibSettings buildSettings(TelegramUserSession session) {
        TDLibSettings settings = TDLibSettings.create(new APIToken(properties.apiId(), properties.apiHash()));
        settings.setDatabaseDirectoryPath(Path.of(session.getTdlibDatabasePath()));
        settings.setDownloadedFilesDirectoryPath(Path.of(session.getTdlibFilesPath()));
        settings.setFileDatabaseEnabled(true);
        settings.setChatInfoDatabaseEnabled(true);
        settings.setMessageDatabaseEnabled(true);
        settings.setSystemLanguageCode(properties.systemLanguageCode());
        settings.setDeviceModel(properties.deviceModel());
        settings.setSystemVersion(properties.systemVersion());
        settings.setApplicationVersion(properties.applicationVersion());
        return settings;
    }

    private ClientInteraction buildInteraction(Long sessionId, ClientRuntime runtime) {
        return (parameter, info) -> switch (parameter) {
            case ASK_CODE -> {
                sessionService.updateState(sessionId, TelegramUserSessionState.WAIT_CODE, null);
                yield runtime.resetCodeFuture();
            }
            case ASK_PASSWORD -> {
                String hint = info instanceof ParameterInfoPasswordHint passwordHint && StringUtils.hasText(passwordHint.getHint())
                        ? "Password hint: " + passwordHint.getHint()
                        : null;
                sessionService.updateState(sessionId, TelegramUserSessionState.WAIT_PASSWORD, hint);
                yield runtime.resetPasswordFuture();
            }
            default -> CompletableFuture.failedFuture(new IllegalStateException("Unsupported Telegram auth parameter: " + parameter));
        };
    }

    private void handleAuthorizationState(Long sessionId, TdApi.UpdateAuthorizationState update) {
        try {
            if (update.authorizationState instanceof TdApi.AuthorizationStateWaitCode) {
                sessionService.updateState(sessionId, TelegramUserSessionState.WAIT_CODE, null);
                return;
            }
            if (update.authorizationState instanceof TdApi.AuthorizationStateWaitPassword) {
                sessionService.updateState(sessionId, TelegramUserSessionState.WAIT_PASSWORD, null);
                return;
            }
            if (update.authorizationState instanceof TdApi.AuthorizationStateReady) {
                ClientRuntime runtime = getRuntime(sessionId);
                runtime.client.getMeAsync()
                        .thenAccept(user -> {
                            sessionService.markReady(sessionId, user.id);
                            runtime.client.loadChatListMainAsync()
                                    .exceptionally(ex -> {
                                        log.warn("Failed to preload Telegram main chat list for session {}", sessionId, ex);
                                        return null;
                                    });
                        })
                        .exceptionally(ex -> {
                            sessionService.markFailed(sessionId, "Telegram authorization completed, but account lookup failed");
                            log.error("Failed to resolve Telegram account for session {}", sessionId, ex);
                            return null;
                        });
                return;
            }
            if (update.authorizationState instanceof TdApi.AuthorizationStateClosed) {
                closeRuntime(sessionId);
            }
        } catch (RuntimeException ex) {
            sessionService.markFailed(sessionId, ex.getMessage());
            throw ex;
        }
    }

    private void handleNewMessage(Long sessionId, TdApi.Message message, String eventType) {
        try {
            TelegramUserSession session = sessionService.getSession(sessionId);
            ClientRuntime runtime = getRuntime(sessionId);
            TdApi.Chat chat = runtime.client.send(new TdApi.GetChat(message.chatId)).join();
            TdApi.User senderUser = resolveSenderUser(runtime.client, message.senderId);
            TdApi.Chat senderChat = resolveSenderChat(runtime.client, message.senderId);
            integrationIngestionService.ingest(inboundEventMapper.map(session, chat, message, senderUser, senderChat, eventType));
            sessionService.touchSync(sessionId);
        } catch (RuntimeException ex) {
            sessionService.markFailed(sessionId, ex.getMessage());
            log.error("Failed to ingest Telegram TDLib message for session {}", sessionId, ex);
            throw ex;
        }
    }

    private void handleEditedMessage(Long sessionId, TdApi.UpdateMessageEdited update) {
        ClientRuntime runtime = getRuntime(sessionId);
        runtime.client.send(new TdApi.GetMessage(update.chatId, update.messageId))
                .thenAccept(message -> handleNewMessage(sessionId, message, "message_edited"))
                .exceptionally(ex -> {
                    sessionService.markFailed(sessionId, "Failed to reload edited Telegram message");
                    log.error("Failed to fetch edited Telegram message for session {}", sessionId, ex);
                    return null;
                });
    }

    private TdApi.User resolveSenderUser(SimpleTelegramClient client, TdApi.MessageSender senderId) {
        if (senderId instanceof TdApi.MessageSenderUser senderUser) {
            return client.send(new TdApi.GetUser(senderUser.userId)).join();
        }
        return null;
    }

    private TdApi.Chat resolveSenderChat(SimpleTelegramClient client, TdApi.MessageSender senderId) {
        if (senderId instanceof TdApi.MessageSenderChat senderChat) {
            return client.send(new TdApi.GetChat(senderChat.chatId)).join();
        }
        return null;
    }

    private ClientRuntime getRuntime(Long sessionId) {
        ClientRuntime runtime = runtimes.get(sessionId);
        if (runtime == null) {
            throw new IllegalStateException("Telegram TDLib runtime is not started for session " + sessionId);
        }
        return runtime;
    }

    private void closeRuntime(Long sessionId) {
        ClientRuntime runtime = runtimes.remove(sessionId);
        if (runtime != null) {
            runtime.closeSilently();
        }
    }

    private void ensureEnabled() {
        if (!properties.enabled()) {
            throw new TelegramUserModeDisabledException("Telegram user mode is disabled");
        }
        if (properties.apiId() <= 0 || !StringUtils.hasText(properties.apiHash())) {
            throw new TelegramUserModeDisabledException("Telegram user api-id and api-hash must be configured");
        }
    }

    private void configureProxy(SimpleTelegramClient client) {
        if (!properties.proxyEnabled()) {
            return;
        }
        if (!StringUtils.hasText(properties.proxyHost()) || properties.proxyPort() <= 0) {
            throw new TelegramUserModeDisabledException("Telegram user proxy host and port must be configured");
        }

        TdApi.Proxy proxy = client.send(new TdApi.AddProxy(
                properties.proxyHost().trim(),
                properties.proxyPort(),
                true,
                new TdApi.ProxyTypeSocks5(
                        StringUtils.hasText(properties.proxyUsername()) ? properties.proxyUsername() : "",
                        StringUtils.hasText(properties.proxyPassword()) ? properties.proxyPassword() : ""
                )
        )).join();
        client.send(new TdApi.EnableProxy(proxy.id)).join();
        log.info("Configured Telegram TDLib SOCKS5 proxy {}:{}", properties.proxyHost(), properties.proxyPort());
    }

    private static final class ClientRuntime {
        private final AtomicReference<CompletableFuture<String>> codeFutureRef = new AtomicReference<>(new CompletableFuture<>());
        private final AtomicReference<CompletableFuture<String>> passwordFutureRef = new AtomicReference<>(new CompletableFuture<>());
        private volatile SimpleTelegramClient client;

        private CompletableFuture<String> ensureCodeFuture() {
            CompletableFuture<String> current = codeFutureRef.get();
            if (current.isDone()) {
                CompletableFuture<String> replacement = new CompletableFuture<>();
                if (codeFutureRef.compareAndSet(current, replacement)) {
                    return replacement;
                }
                return codeFutureRef.get();
            }
            return current;
        }

        private CompletableFuture<String> ensurePasswordFuture() {
            CompletableFuture<String> current = passwordFutureRef.get();
            if (current.isDone()) {
                CompletableFuture<String> replacement = new CompletableFuture<>();
                if (passwordFutureRef.compareAndSet(current, replacement)) {
                    return replacement;
                }
                return passwordFutureRef.get();
            }
            return current;
        }

        private CompletableFuture<String> resetCodeFuture() {
            CompletableFuture<String> future = new CompletableFuture<>();
            codeFutureRef.set(future);
            return future;
        }

        private CompletableFuture<String> resetPasswordFuture() {
            CompletableFuture<String> future = new CompletableFuture<>();
            passwordFutureRef.set(future);
            return future;
        }

        private void closeSilently() {
            if (client != null) {
                client.closeAsync();
            }
        }
    }
}
