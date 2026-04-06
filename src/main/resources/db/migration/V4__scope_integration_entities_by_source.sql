alter table integration_source
    add column source_external_id varchar(255);

alter table integration_source
    add constraint uk_integration_source_platform_external unique (platform, source_external_id);

alter table conversation
    drop constraint uk_conversation_platform_external;

alter table conversation
    add constraint uk_conversation_source_external unique (source_id, external_conversation_id);

alter table message
    drop constraint uk_message_platform_external;

alter table message
    add constraint uk_message_conversation_external unique (conversation_id, external_message_id);

alter table external_user
    add column source_id bigint references integration_source(id) on delete cascade;

alter table external_user
    drop constraint uk_external_user_platform_external;

alter table external_user
    add constraint uk_external_user_source_external unique (source_id, external_user_id);
