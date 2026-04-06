create table vk_crawl_job (
    id bigserial primary key,
    source_id bigint references integration_source(id) on delete cascade,
    job_type varchar(64) not null,
    status varchar(32) not null,
    request_json text not null,
    result_json text,
    item_count integer not null default 0,
    processed_count integer not null default 0,
    error_count integer not null default 0,
    warning_count integer not null default 0,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table vk_group_candidate (
    id bigserial primary key,
    vk_group_id bigint not null,
    source_id bigint references integration_source(id) on delete cascade,
    screen_name varchar(255),
    name varchar(255),
    region_match_source varchar(32) not null,
    collection_method varchar(32) not null,
    raw_json text not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_vk_group_candidate_vk_group_id unique (vk_group_id)
);

create table vk_user_candidate (
    id bigserial primary key,
    vk_user_id bigint not null,
    source_id bigint references integration_source(id) on delete cascade,
    display_name varchar(255),
    first_name varchar(255),
    last_name varchar(255),
    profile_url varchar(1024),
    region_match_source varchar(32) not null,
    collection_method varchar(32) not null,
    raw_json text not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_vk_user_candidate_vk_user_id unique (vk_user_id)
);

create table vk_wall_post_snapshot (
    id bigserial primary key,
    owner_id bigint not null,
    post_id bigint not null,
    source_id bigint references integration_source(id) on delete cascade,
    author_vk_user_id bigint,
    text text,
    collection_method varchar(32) not null,
    raw_json text not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_vk_wall_post_snapshot_owner_post unique (owner_id, post_id)
);

create table vk_comment_snapshot (
    id bigserial primary key,
    owner_id bigint not null,
    post_id bigint not null,
    comment_id bigint not null,
    source_id bigint references integration_source(id) on delete cascade,
    author_vk_user_id bigint,
    text text,
    collection_method varchar(32) not null,
    raw_json text not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_vk_comment_snapshot_owner_post_comment unique (owner_id, post_id, comment_id)
);
