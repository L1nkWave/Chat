drop table if exists deactivated_tokens;

create table deactivated_tokens
(
    id         uuid primary key,
    expiration timestamptz not null check ( expiration > now() )
);