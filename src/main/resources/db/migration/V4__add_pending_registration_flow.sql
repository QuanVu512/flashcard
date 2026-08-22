create table auth_pending_registrations (
    id uuid primary key,
    email varchar(180) not null,
    display_name varchar(120) not null,
    password_hash varchar(255) not null,
    created_at timestamp not null,
    expires_at timestamp not null,
    completed_at timestamp
);

create index idx_pending_registration_email
    on auth_pending_registrations(email);
create index idx_pending_registration_expires
    on auth_pending_registrations(expires_at);

alter table auth_otp_challenges alter column user_id drop not null;
alter table auth_otp_challenges
    add column pending_registration_id uuid references auth_pending_registrations(id) on delete cascade;
alter table auth_otp_challenges add column subject_key_hash varchar(64);
update auth_otp_challenges
set subject_key_hash = cast(user_id as varchar)
where subject_key_hash is null;
alter table auth_otp_challenges alter column subject_key_hash set not null;
alter table auth_otp_challenges
    add constraint ck_otp_challenge_single_subject check (
        (user_id is not null and pending_registration_id is null and purpose <> 'REGISTRATION')
        or (user_id is null and pending_registration_id is not null and purpose = 'REGISTRATION')
    );

create index idx_auth_otp_pending_created
    on auth_otp_challenges(pending_registration_id, created_at desc);

alter table auth_otp_mail_deliveries alter column user_id drop not null;
alter table auth_otp_mail_deliveries add column quota_key_hash varchar(64);
update auth_otp_mail_deliveries
set quota_key_hash = cast(user_id as varchar)
where quota_key_hash is null;
alter table auth_otp_mail_deliveries alter column quota_key_hash set not null;
create index idx_otp_delivery_quota_created
    on auth_otp_mail_deliveries(quota_key_hash, created_at);

alter table auth_otp_browser_blocks alter column user_id drop not null;
alter table auth_otp_browser_blocks add column subject_key_hash varchar(64);
update auth_otp_browser_blocks
set subject_key_hash = cast(user_id as varchar)
where subject_key_hash is null;
alter table auth_otp_browser_blocks alter column subject_key_hash set not null;
alter table auth_otp_browser_blocks
    add constraint uk_otp_browser_block_subject unique (subject_key_hash, client_key_hash);
