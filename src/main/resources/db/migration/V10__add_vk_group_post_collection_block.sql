alter table vk_group_candidate
    add column if not exists post_collection_blocked_until timestamptz;
