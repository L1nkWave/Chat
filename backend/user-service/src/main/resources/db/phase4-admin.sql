-- Giai doan 4 - P1: nhat ky he thong. Chay 1 lan tren DB lw-users. Idempotent.
create table if not exists audit_logs (
    id           bigserial primary key,
    actor_id     bigint       not null,
    action       varchar(64)  not null,
    target_type  varchar(32),
    target_id    varchar(64),
    detail       text,
    ip           varchar(64),
    created_at   timestamptz  not null default now()
);

create index if not exists idx_audit_logs_created on audit_logs (created_at desc);
create index if not exists idx_audit_logs_actor on audit_logs (actor_id);