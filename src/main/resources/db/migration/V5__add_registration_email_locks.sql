create table auth_registration_email_locks (
    email_key_hash varchar(64) primary key,
    created_at timestamp not null
);
