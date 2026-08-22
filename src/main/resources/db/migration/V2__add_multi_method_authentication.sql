alter table users add column if not exists email_verified boolean not null default false;
alter table users alter column password_hash drop not null;

create table if not exists auth_identities (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    provider varchar(24) not null,
    issuer varchar(255) not null,
    subject varchar(255) not null,
    provider_email varchar(180),
    email_verified boolean not null default false,
    created_at timestamp not null,
    constraint uk_auth_identity_subject unique (provider, issuer, subject),
    constraint uk_auth_identity_user_provider unique (user_id, provider)
);

insert into auth_identities (
    id, user_id, provider, issuer, subject, provider_email, email_verified, created_at
)
select id, id, 'LOCAL', 'flashcard', email, email, email_verified, created_at
from users
where not exists (
    select 1 from auth_identities ai
    where ai.provider = 'LOCAL' and ai.user_id = users.id
);

create table if not exists auth_refresh_sessions (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    token_hash varchar(64) not null,
    auth_method varchar(24) not null,
    expires_at timestamp not null,
    revoked_at timestamp,
    created_at timestamp not null,
    last_used_at timestamp not null
);

create table if not exists auth_otp_challenges (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    purpose varchar(32) not null,
    code_hash varchar(64) not null,
    attempts integer not null default 0,
    expires_at timestamp not null,
    consumed_at timestamp,
    created_at timestamp not null,
    sent_at timestamp not null
);

create table if not exists auth_trusted_devices (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    token_hash varchar(64) not null,
    user_agent_hash varchar(64),
    expires_at timestamp not null,
    revoked_at timestamp,
    created_at timestamp not null,
    last_used_at timestamp not null
);

create index if not exists idx_auth_identity_user on auth_identities(user_id);
create index if not exists idx_auth_refresh_user_expires on auth_refresh_sessions(user_id, expires_at);
create index if not exists idx_auth_otp_user_created on auth_otp_challenges(user_id, created_at desc);
create index if not exists idx_auth_trusted_user_expires on auth_trusted_devices(user_id, expires_at);
