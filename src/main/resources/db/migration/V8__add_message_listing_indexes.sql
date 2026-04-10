create index if not exists idx_message_sent_at_desc on message (sent_at desc, id desc);

create index if not exists idx_message_platform_sent_at_desc on message (platform, sent_at desc, id desc);

create index if not exists idx_message_conversation_sent_at_desc on message (conversation_id, sent_at desc, id desc);

create index if not exists idx_message_author_sent_at_desc on message (author_id, sent_at desc, id desc);
