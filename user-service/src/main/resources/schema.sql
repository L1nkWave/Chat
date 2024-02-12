begin;

drop table if exists contacts;
drop table if exists user_roles;
drop table if exists roles;
drop table if exists users;

create table users
(
    id          bigserial primary key,
    username    varchar(32) not null unique,
    password    text        not null,
    name        varchar(64) not null,
    avatar_path text,
    created_at  timestamptz not null default now(),
    last_seen   timestamptz not null default now() + interval '1' second,
    theme       boolean     not null default false,
    is_online   boolean     not null default false,
    bio         text,

    check (char_length(username) >= 3),
    check (char_length(name) >= 3),
    check (last_seen > created_at)
);

create table roles
(
    id   serial primary key,
    name varchar(32) unique not null
);

create table user_roles
(
    user_id bigint references users not null,
    role_id int references roles    not null,
    primary key (user_id, role_id)
);

insert into roles (name)
values ('ROLE_USER'),
       ('ROLE_ADMIN');

insert into users (username, password, name, avatar_path, created_at, last_seen, theme, is_online, bio)
values ('mathew',
        '$2a$12$QH/Sw8dKw5naMRtGNHcgHO0A/YlyLeUAG0OdZndQlADLdmk0sXk6e',
        'Matthew McConaughey',
        null,
        default,
        default,
        default,
        default,
        null);

insert into users (username, password, name, avatar_path)
values ('user',
        '$2a$12$QH/Sw8dKw5naMRtGNHcgHO0A/YlyLeUAG0OdZndQlADLdmk0sXk6e',
        'User',
        null);

insert into user_roles (user_id, role_id)
values (1, 1),
       (1, 2),
       (2, 1);

create table contacts
(
    id        serial primary key,
    user_id_1 bigint references users not null,
    user_id_2 bigint references users not null,
    added_at  timestamptz             not null default now(),
    alias     varchar(64)             not null,
    unique (user_id_1, user_id_2)
);

insert into contacts (user_id_1, user_id_2, added_at, alias)
values (1, 2, default, 'Mr. User');

commit;