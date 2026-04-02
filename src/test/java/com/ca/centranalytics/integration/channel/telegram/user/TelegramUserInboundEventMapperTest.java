package com.ca.centranalytics.integration.channel.telegram.user;

import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSession;
import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSessionState;
import com.ca.centranalytics.integration.channel.telegram.user.service.TelegramUserInboundEventMapper;
import com.ca.centranalytics.integration.domain.entity.ConversationType;
import com.ca.centranalytics.integration.domain.entity.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramUserInboundEventMapperTest {

    private final TelegramUserInboundEventMapper mapper = new TelegramUserInboundEventMapper(new ObjectMapper());

    @Test
    void mapsTdLibUserMessageIntoInboundEvent() {
        TelegramUserSession session = TelegramUserSession.builder()
                .id(7L)
                .phoneNumber("+79990001122")
                .telegramUserId(7001L)
                .sessionState(TelegramUserSessionState.READY)
                .authorized(true)
                .tdlibDatabasePath(".tdlib/db/79990001122")
                .tdlibFilesPath(".tdlib/files/79990001122")
                .build();

        TdApi.Chat chat = new TdApi.Chat();
        chat.id = -1001234567890L;
        chat.title = "TDLib Group";
        chat.type = new TdApi.ChatTypeSupergroup(11L, false);

        TdApi.User sender = new TdApi.User();
        sender.id = 55L;
        sender.firstName = "Ivan";
        sender.lastName = "Petrov";
        sender.phoneNumber = "79990001122";
        sender.usernames = new TdApi.Usernames(new String[]{"ivan_petrov"}, new String[0], "ivan_petrov");

        TdApi.Message message = new TdApi.Message();
        message.id = 501L;
        message.chatId = chat.id;
        message.date = 1775088000;
        message.senderId = new TdApi.MessageSenderUser(sender.id);
        message.content = new TdApi.MessageText(new TdApi.FormattedText("Hello from TDLib", new TdApi.TextEntity[0]), null, null);
        message.replyTo = new TdApi.MessageReplyToMessage(chat.id, 500L, null, 0, null, 0, null);

        var event = mapper.map(session, chat, message, sender, null, "message_new");

        assertThat(event.platform().name()).isEqualTo("TELEGRAM");
        assertThat(event.conversation().type()).isEqualTo(ConversationType.GROUP);
        assertThat(event.author().externalUserId()).isEqualTo("55");
        assertThat(event.message().messageType()).isEqualTo(MessageType.TEXT);
        assertThat(event.message().replyToExternalMessageId()).isEqualTo("500");
        assertThat(event.message().text()).isEqualTo("Hello from TDLib");
        assertThat(event.sourceSettingsJson()).contains("\"mode\":\"user\"");
    }
}
