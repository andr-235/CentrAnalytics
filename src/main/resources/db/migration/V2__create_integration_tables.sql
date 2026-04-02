create table integration_source (
    id bigserial primary key,
    platform varchar(32) not null,
    name varchar(255) not null,
    status varchar(32) not null,
    settings_json text,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table integration_account (
    id bigserial primary key,
    source_id bigint not null references integration_source(id) on delete cascade,
    external_account_id varchar(255) not null,
    display_name varchar(255),
    username varchar(255),
    metadata_json text,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_integration_account_source_external unique (source_id, external_account_id)
);

create table conversation (
    id bigserial primary key,
    source_id bigint not null references integration_source(id) on delete cascade,
    platform varchar(32) not null,
    external_conversation_id varchar(255) not null,
    type varchar(32) not null,
    title varchar(255),
    metadata_json text,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_conversation_platform_external unique (platform, external_conversation_id)
);

create table external_user (
    id bigserial primary key,
    platform varchar(32) not null,
    external_user_id varchar(255) not null,
    display_name varchar(255),
    username varchar(255),
    first_name varchar(255),
    last_name varchar(255),
    phone varchar(64),
    profile_url varchar(1024),
    is_bot boolean not null default false,
    metadata_json text,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_external_user_platform_external unique (platform, external_user_id)
);

create table raw_event (
    id bigserial primary key,
    platform varchar(32) not null,
    event_type varchar(255) not null,
    event_id varchar(255) not null,
    received_at timestamp with time zone not null,
    payload_json text not null,
    signature_valid boolean not null default false,
    processing_status varchar(32) not null,
    error_message text,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_raw_event_platform_event unique (platform, event_id)
);

create table message (
    id bigserial primary key,
    conversation_id bigint not null references conversation(id) on delete cascade,
    platform varchar(32) not null,
    external_message_id varchar(255) not null,
    author_id bigint references external_user(id) on delete set null,
    sent_at timestamp with time zone not null,
    text text,
    normalized_text text,
    message_type varchar(32) not null,
    reply_to_external_message_id varchar(255),
    forwarded_from varchar(255),
    has_attachments boolean not null default false,
    raw_event_id bigint not null references raw_event(id) on delete cascade,
    ingestion_status varchar(32) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_message_platform_external unique (platform, external_message_id)
);

create table message_attachment (
    id bigserial primary key,
    message_id bigint not null references message(id) on delete cascade,
    attachment_type varchar(64) not null,
    external_attachment_id varchar(255),
    url varchar(2048),
    mime_type varchar(255),
    metadata_json text,
    created_at timestamp with time zone not null default current_timestamp
);

create table ingestion_checkpoint (
    id bigserial primary key,
    source_id bigint not null references integration_source(id) on delete cascade,
    checkpoint_type varchar(64) not null,
    checkpoint_value varchar(1024),
    last_success_at timestamp with time zone,
    last_failure_at timestamp with time zone,
    status varchar(32) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_ingestion_checkpoint_source_type unique (source_id, checkpoint_type)
);
