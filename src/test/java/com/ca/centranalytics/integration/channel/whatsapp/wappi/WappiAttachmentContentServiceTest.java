package com.ca.centranalytics.integration.channel.whatsapp.wappi;

import com.ca.centranalytics.integration.channel.whatsapp.wappi.service.WappiAttachmentContentService;
import com.ca.centranalytics.integration.domain.entity.MessageAttachment;
import com.ca.centranalytics.integration.ingestion.dto.InboundAttachment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WappiAttachmentContentServiceTest {

    private final WappiAttachmentContentService service = new WappiAttachmentContentService();

    @Test
    void decodesAttachmentBase64AndBuildsBinaryContent() {
        MessageAttachment attachment = MessageAttachment.builder()
                .id(77L)
                .attachmentType("document")
                .build();

        var content = service.buildContent(attachment, new InboundAttachment(
                "document",
                "doc-1",
                null,
                "application/pdf",
                "{}",
                "1.pdf",
                "AQIDBA=="
        ));

        assertThat(content).isPresent();
        assertThat(content.get().getAttachment()).isSameAs(attachment);
        assertThat(content.get().getFileName()).isEqualTo("1.pdf");
        assertThat(content.get().getContentSize()).isEqualTo(4L);
        assertThat(content.get().getContent()).containsExactly(1, 2, 3, 4);
        assertThat(content.get().getContentSha256()).isNotBlank();
    }

    @Test
    void rejectsMalformedBase64Payload() {
        MessageAttachment attachment = MessageAttachment.builder()
                .id(78L)
                .attachmentType("document")
                .build();

        assertThatThrownBy(() -> service.buildContent(attachment, new InboundAttachment(
                "document",
                "doc-2",
                null,
                "application/pdf",
                "{}",
                "broken.pdf",
                "%%%not-base64%%%"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("base64");
    }
}
