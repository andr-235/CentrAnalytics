create table telegram_user_session (
    id bigserial primary key,
    phone_number varchar(64) not null,
    telegram_user_id bigint,
    session_state varchar(32) not null,
    tdlib_database_path varchar(1024) not null,
    tdlib_files_path varchar(1024) not null,
    is_authorized boolean not null default false,
    last_sync_at timestamp with time zone,
    error_message text,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);
