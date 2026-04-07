create table message_attachment_content (
    id bigserial primary key,
    attachment_id bigint not null unique references message_attachment(id) on delete cascade,
    content bytea not null,
    file_name varchar(512),
    content_size bigint not null,
    content_sha256 varchar(64),
    created_at timestamp with time zone not null default current_timestamp
);
