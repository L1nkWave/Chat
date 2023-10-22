drop table if exists forward_messages;
drop table if exists messages;
drop table if exists user_chat;
drop table if exists chats;
drop table if exists user_roles;
drop table if exists roles;
drop table if exists users;

create table users
(
    id            bigserial primary key,
    username      varchar(32) not null unique,
    password      text        not null,
    refresh_token text,
    name          varchar(64) not null,
    avatar_path   text        not null,
    created_at    timestamptz not null default now(),
    last_seen     timestamptz not null,
    theme         boolean     not null default false,

    check (char_length(username) >= 3),
    check (char_length(name) >= 3)
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

insert into users (username, password, name, avatar_path, created_at, last_seen, theme)
values ('maTTheW',
        '123',
        'Matthew McConaughey',
        '',
        default,
        now(),
        default);

insert into user_roles (user_id, role_id)
values (1, 1),
       (1, 2);


create table chats
(
    id            bigserial primary key,
    name          varchar(128) not null,
    type          varchar(32)  not null,
    avatar_path   text         not null,
    description   text,
    members_limit bigint       not null,
    is_private    boolean      not null default false,
    check (char_length(name) > 0)
);

insert into chats (name, type, avatar_path, description, members_limit)
values ('CS-0', 'GROUP', '', null, 10);

create table user_chat
(
    user_id   bigint references users not null,
    chat_id   bigint references chats not null,
    joined_at timestamptz                      default now(),
    role      varchar(32)             not null default 'default',
    primary key (user_id, chat_id)
);

insert into user_chat (user_id, chat_id)
values (1, 1);

create table messages
(
    id         bigserial primary key,
    chat_id    bigint references chats not null,
    sender_id  bigint references users not null,
    text       text                    not null,
    is_file    boolean                 not null,
    is_read    boolean                 not null default false,
    created_at timestamptz             not null default now()
);

create table forward_messages
(
    id             bigserial primary key,
    message_id     bigint references messages not null,
    target_chat_id bigint references chats    not null,
    forwarded_id   bigint references users    not null,
    text           text                       not null,
    is_read        boolean                    not null default false,
    created_at     timestamptz                not null default now()
);

insert into messages (chat_id, sender_id, text, is_file)
values (1, 1, 'hello world', false);

insert into forward_messages (message_id, target_chat_id, forwarded_id, text)
values (1, 1, 1, 'I forwarded message made by myself');