create table if not exists clients (
    id uuid primary key,
    display_name varchar(120) not null,
    created_at timestamp not null,
    score bigint not null default 0
);

create table if not exists users (
    id uuid primary key,
    email varchar(180) not null unique,
    password_hash varchar(255) not null,
    role varchar(40) not null default 'ROLE_USER',
    enabled boolean not null default true,
    created_at timestamp not null,
    client_id uuid not null unique references clients(id)
);

create table if not exists folders (
    id uuid primary key,
    name varchar(140) not null,
    description varchar(280),
    created_at timestamp not null,
    client_id uuid not null references clients(id)
);

create table if not exists flashcard_sets (
    id uuid primary key,
    title varchar(180) not null,
    description varchar(600),
    created_at timestamp not null,
    updated_at timestamp not null,
    client_id uuid not null references clients(id),
    folder_id uuid references folders(id)
);

create table if not exists flashcards (
    id uuid primary key,
    term varchar(240) not null,
    definition varchar(800) not null,
    phonetic varchar(240),
    example varchar(800),
    position integer not null,
    flashcard_set_id uuid not null references flashcard_sets(id) on delete cascade
);

create index if not exists idx_folders_client_created_at
    on folders(client_id, created_at desc);

create index if not exists idx_flashcard_sets_client_created_at
    on flashcard_sets(client_id, created_at desc);

create index if not exists idx_flashcard_sets_folder
    on flashcard_sets(folder_id);

create index if not exists idx_flashcards_set_position
    on flashcards(flashcard_set_id, position);
