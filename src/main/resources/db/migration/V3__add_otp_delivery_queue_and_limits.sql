alter table auth_otp_challenges add column if not exists client_key_hash varchar(64);
alter table auth_otp_challenges add column if not exists resend_available_at timestamp;

create table if not exists auth_otp_mail_deliveries (
    id uuid primary key,
    challenge_id uuid not null references auth_otp_challenges(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    recipient varchar(180) not null,
    purpose varchar(32) not null,
    encrypted_code varchar(512) not null,
    status varchar(24) not null,
    attempts integer not null default 0,
    available_at timestamp not null,
    created_at timestamp not null,
    sent_at timestamp,
    last_error varchar(500)
);

create table if not exists auth_otp_browser_blocks (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    client_key_hash varchar(64) not null,
    blocked_until timestamp not null,
    updated_at timestamp not null,
    constraint uk_otp_browser_block unique (user_id, client_key_hash)
);

create index if not exists idx_otp_delivery_ready
    on auth_otp_mail_deliveries(status, available_at, created_at);
create index if not exists idx_otp_delivery_user_created
    on auth_otp_mail_deliveries(user_id, created_at);
create index if not exists idx_otp_delivery_challenge
    on auth_otp_mail_deliveries(challenge_id, created_at);
create index if not exists idx_otp_browser_block_until
    on auth_otp_browser_blocks(blocked_until);
