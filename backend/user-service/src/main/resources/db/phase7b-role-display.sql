-- Giai doan 7b - Ten hien thi tieng Viet cho vai tro (dung chung DB lw-users)
-- Chay 1 lan tren DB lw-users sau phase7-user-profiles.sql. Idempotent.

alter table roles add column if not exists display_name varchar(128);

update roles set display_name = 'Quản trị cao nhất' where name = 'ROLE_ROOT' and display_name is null;
update roles set display_name = 'Quản trị viên'     where name = 'ROLE_ADMIN' and display_name is null;
update roles set display_name = 'Giảng viên'        where name = 'ROLE_LECTURER' and display_name is null;
update roles set display_name = 'Sinh viên'         where name = 'ROLE_STUDENT' and display_name is null;
update roles set display_name = 'Người dùng'        where name = 'ROLE_USER' and display_name is null;