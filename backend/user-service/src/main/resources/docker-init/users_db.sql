start transaction;

create table roles
(
    id   serial primary key,
    name varchar(32) unique not null
);

insert into roles (name)
values ('ROLE_USER'),
       ('ROLE_ADMIN');

commit;