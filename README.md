# SGU Chat

Chat nội bộ cho Đại học Sài Gòn, kiểu Zalo: chat riêng, nhóm lớp, nhóm môn, nhóm khoa được tạo từ dữ liệu đào tạo, kèm bot tra cứu và trang quản trị học vụ. Đồ án môn Seminar chuyên đề, không phải sản phẩm chính thức của nhà trường.

## Tính năng

- **Chat**: chat riêng, nhóm tự tạo, nhóm lớp, nhóm môn, nhóm khoa. Mọi thành viên nhóm phải chấp nhận lời mời. Ghim tối đa 5 cuộc trò chuyện, lọc theo loại, tìm người để nhắn riêng theo tên hoặc mã.
- **Tin nhắn**: gửi nhiều tệp cùng lúc, thư viện ảnh và tệp của từng cuộc trò chuyện, xóa tin (tác giả hoặc admin nhóm). Liên kết cần xác nhận trước khi mở.
- **Bot**: `/tkb` (thời khóa biểu), `/lichthi`, `/lop`, `/lichgac`.
- **Tài khoản**: hồ sơ, đổi số điện thoại và địa chỉ, đổi mật khẩu, đăng xuất mọi thiết bị.
- **Xác thực hai lớp (2FA)**: bật cho admin, có mã khôi phục.
- **Quản trị học vụ**: import Excel (khoa, lớp, sinh viên, giảng viên, thời khóa biểu, học kỳ, môn học, lịch thi, lịch gác thi), đồng bộ nhóm học vụ theo bấm nút, quản lý người dùng, vai trò, phân quyền, báo cáo tin nhắn, nhật ký hệ thống.

Tài khoản do nhà trường cấp. Không có đăng ký công khai.

## Kiến trúc

| Thành phần | Công nghệ | Cổng |
|---|---|---|
| Frontend | Next.js (App Router), React, Tailwind CSS v4 | 3000 |
| api-gateway | Spring Cloud Gateway | 8080 |
| auth-service | Spring Boot, Spring Security, Redis (phiên, mã 2FA) | 8081 |
| user-service | Spring Boot, JPA, PostgreSQL, Flyway, Apache POI | 8082 |
| chat-service | Spring Boot, MongoDB, đồng bộ nhóm học vụ | 8083 |
| ws-server | Spring Boot WebSocket, nhận sự kiện từ chat-service (chỉ chạy một instance) | 9090 |
| discovery-service | Eureka | 8761 |
| users-db | PostgreSQL 16 (CSDL `lw-users`) | 15432 |
| chat-db | MongoDB 7 (replica set `rs0`) | 27017 |
| redis-sessions | Redis | 16379 |

- User Service chịu trách nhiệm thực thi Flyway migration cho PostgreSQL lw-users.
- Auth Service sử dụng chung database PostgreSQL nhưng không tự chạy migration.

Frontend chỉ gọi `/api/*` qua api-gateway. Trình duyệt nhận sự kiện realtime qua ws-server bằng ticket lấy từ API.

Cấu trúc thư mục:

```
backend/
  api-gateway/  auth-service/  user-service/  chat-service/  ws-server/  discovery-service/
  shared/       Mã dùng chung (mã hóa CCCD, lỗi, bảo mật nội bộ)
  gen-keys.ps1  Sinh khóa RSA cho auth-service
  test-chats.ps1, fix-spring-security-matchers.ps1, migrate-jackson3.ps1  Công cụ phụ trợ
  docker-compose.yml, .env.example
frontend/
  src/app/        Các trang (chat, admin, account, login, change-password, security/2fa)
  src/components/ Giao diện theo khu vực; components/ui là bộ thành phần dùng chung
  src/lib/        Gọi API, WebSocket, định dạng dữ liệu
test-data/        9 file Excel mẫu để import (xem bên dưới)
scripts/reset/    Script xóa dữ liệu (chỉ chạy khi cần, xem bên dưới)
```

## Yêu cầu

- JDK 21 và Maven 3.9+ (để build và chạy từng service)
- Node.js 20+ và npm (frontend)
- Docker và Docker Compose (chạy cơ sở dữ liệu và, tùy chọn, toàn bộ stack)
- `openssl` để sinh khóa. Trên Windows có thể dùng openssl đi kèm Git for Windows (`<thư mục cài Git>\usr\bin\openssl.exe`). Nếu `openssl version` không chạy được trong PowerShell, thêm thư mục đó vào PATH cho phiên hiện tại trước khi chạy script:
  ```powershell
  $env:Path += ";<thư mục cài Git>\usr\bin"
  ```
  Lệnh này chỉ có tác dụng trong cửa sổ PowerShell hiện tại. Cách khác là chạy `openssl` trong WSL (`sudo apt install -y openssl`).

## Cài đặt

### 1. Sinh khóa và tạo file môi trường

```bash
powershell -ExecutionPolicy Bypass -File .\backend\gen-keys.ps1   # chạy trong PowerShell, tại thư mục gốc; sinh cặp khóa RSA cho auth-service
cp backend/.env.example backend/.env
cp frontend/env.example frontend/.env.local
```

`gen-keys.ps1` chỉ sinh khóa RSA (access và refresh token). Các giá trị còn lại trong `backend/.env` tự tạo bằng `openssl rand -base64 32`:

- `CCCD_ENCRYPTION_KEY` và `CCCD_INDEX_KEY`: mỗi khóa phải giải mã base64 ra đúng 32 byte, nếu không user-service sẽ không khởi động.
- `TOTP_KEY_1`: 32 byte base64. `TOTP_KEY_VERSION` để mặc định là `1`.
- `INTERNAL_SERVICE_KEY`: dùng chung cho mọi service, phải giống nhau.
- `TWO_FACTOR_CHALLENGE_SECRET`: chuỗi ngẫu nhiên từ 32 ký tự trở lên.

Không đổi các khóa này khi đã có dữ liệu: đổi `TOTP_KEY_1` sẽ làm không đọc được secret 2FA đã lưu, và đổi khóa CCCD làm không đọc được CCCD đã mã hóa. Không commit `.env` thật.

### 2. Chạy cơ sở dữ liệu

```bash
cd backend
docker compose up -d --no-deps users-db redis-sessions discovery-service chat-db
```

Sau khi chat-db healthy:
```bash
docker compose up -d --no-deps mongo-init
```
`--no-deps` bắt buộc ở đây: `chat-db` trong compose phụ thuộc auth-service, nếu không có cờ này compose sẽ kéo cả auth và user lên và chiếm cổng 8081, 8082 trước khi chạy Maven. Service `mongo-init` khởi tạo replica set `rs0` sau khi `chat-db` sẵn sàng. Kiểm tra bằng `docker compose logs mongo-init`.

### 3. Chạy backend

Có hai cách.

**Cách A, toàn bộ bằng Docker:**

```bash
cd backend
docker compose up -d --build
```

Flyway tự động chạy khi User Service khởi động. Không cần chạy SQL thủ công.

Compose chỉ chạy **một** ws-server (container `ws-server`, cổng 9090). Không thêm instance thứ hai khi phát triển: mỗi instance giữ kết nối WebSocket trong bộ nhớ riêng, và việc chuyển tin giữa các instance chưa hoạt động đúng, nên tin realtime (tin nhắn, lời mời, tin của bot) sẽ không đến đều.

**Cách B, từng service bằng Maven** (mở từng terminal, theo đúng thứ tự):

Maven không đọc `backend/.env`, và file đó dùng tên service Docker (`users-db`, `discovery-service`) không phân giải được trên máy. Trong mỗi terminal chạy service, khai báo biến như sau (thay giá trị bằng khóa đã tạo ở bước 1):

```bash
export INTERNAL_SERVICE_KEY=...
export CCCD_ENCRYPTION_KEY=...
export CCCD_INDEX_KEY=...
export TOTP_KEY_1=...
export TOTP_KEY_VERSION=1
export TWO_FACTOR_CHALLENGE_SECRET=...
export DISCOVERY_URL=http://localhost:8761/eureka/
```

Riêng ws-server, khai báo thêm biến này trước khi chạy (tắt broadcast giữa các instance, vì chỉ có một instance):

```bash
export SERVER_INSTANCES_ENABLED=false
```

Sau đó:

```bash
cd backend/discovery-service && mvn spring-boot:run
cd backend/user-service      && mvn spring-boot:run
cd backend/auth-service      && mvn spring-boot:run
cd backend/chat-service      && mvn spring-boot:run
cd backend/ws-server         && mvn spring-boot:run
cd backend/api-gateway       && mvn spring-boot:run
```

Flyway cũng tự động chạy trong quá trình khởi động User Service.

Kiểm tra: `http://localhost:8761` (Eureka) phải thấy đủ các service đã đăng ký.

### 4. Chạy frontend

```bash
cd frontend
npm ci
npm run dev        # http://localhost:3000
```

Lập trình viên tự tạo tài khoản root trong môi trường local. Đổi mật khẩu ngay sau lần đăng nhập đầu tiên.

## Database Schema Migration (Flyway)
A. Cách hoạt động
- Flyway được tích hợp tại User Service.
- Migration nằm trong backend/user-service/src/main/resources/db/migration/.
- Migration tự động chạy khi User Service khởi động.
- Các migration đã thực hiện được lưu trong flyway_schema_history.
- Auth Service dùng chung PostgreSQL nhưng không thực thi migration riêng.
- Hibernate ddl-auto: validate để kiểm tra schema.

B. Danh sách migration

| Migration | Nội dung |
|-----------|---|
| V1        | Tạo users, roles, user_roles, contacts |
| V2        | Tạo permissions, role_permissions, scoped_roles, audit_logs |
| V3        | Tạo các bảng học vụ |
| V4        | Tạo user_profiles và các bảng xác thực |
| V5        | Seed roles và permissions |
| V6        | Seed tài khoản sgu_bot |

C. Quy tắc khi phát triển
- Không chỉnh sửa migration đã được thực thi.
- Khi cần thêm bảng, cột hoặc index, tạo migration phiên bản mới.
- Ví dụ migration tiếp theo là V7__...sql.
- Không chạy lại migration đã hoàn thành.
- Không sử dụng Hibernate ddl-auto: update để thay đổi schema.

**Lưu ý:** 
database cũ được tạo trước Flyway không được tự động coi là database mới. Phải backup, đối chiếu schema và baseline đúng phiên bản trước khi áp dụng.

## Tạo tài khoản root

Chạy sau bước 2 (cần vai trò `ROLE_ROOT` có sẵn). Lệnh hỏi mật khẩu tạm và không hiện ký tự khi gõ. Tài khoản được đặt cờ bắt buộc đổi mật khẩu, nên lần đăng nhập đầu sẽ phải đổi.

```bash
read -s -p "Mật khẩu root tạm (không chứa dấu nháy đơn): " ROOT_PW; echo
docker exec -i users-db psql -U postgres -d lw-users -c "insert into users (username, password, name, must_change_password) values ('root', crypt('$ROOT_PW', gen_salt('bf', 10)), 'Quản trị cao nhất', true) on conflict (username) do update set password = excluded.password, must_change_password = true;"
docker exec -i users-db psql -U postgres -d lw-users -c "insert into user_roles (users_id, roles_id) select u.id, r.id from users u, roles r where u.username = 'root' and r.name = 'ROLE_ROOT' on conflict do nothing;"
unset ROOT_PW
```

Lệnh có thể chạy lại để đặt lại mật khẩu root; mỗi lần chạy sẽ ghi đè mật khẩu cũ.

Sau khi đăng nhập bằng `root`, đổi mật khẩu, rồi vào **Xác thực hai lớp** để bật 2FA cho tài khoản này.

`dataset.sql` ở thư mục `resources` không nằm trong danh sách này và không chạy trên DB thật, vì nó chứa tài khoản mẫu.

## Nhập dữ liệu đào tạo

Flyway không tự động import dữ liệu từ các file Excel trong test-data/. Quản trị viên cần đăng nhập và thực hiện import thông qua giao diện Học vụ theo đúng thứ tự được hướng dẫn.

Đăng nhập bằng tài khoản admin, vào tab **Học vụ**. Import theo đúng thứ tự, vì mỗi file phụ thuộc file trước:

1. `1-khoa.xlsx`
2. `9-giang-vien.xlsx` (trước khi import lớp, để gán GVCN)
3. `2-lop.xlsx`
4. `3-sinh-vien.xlsx`
5. `8-mon-hoc.xlsx`
6. `7-hoc-ky.xlsx`
7. `4-thoi-khoa-bieu.xlsx` (cần môn học đã có từ bước 5; import trước sẽ báo lỗi môn học)
8. `5-lich-thi.xlsx`
9. `6-lich-gac-thi.xlsx`

**Lưu ý:** Sau khi import, bấm **Đồng bộ nhóm học vụ**. Nhóm lớp, môn, khoa được tạo từ dữ liệu này. Đồng bộ là thao tác thủ công của admin; chạy lại không tạo nhóm trùng, nhưng lời mời đã từ chối sẽ được gửi lại.

Các file mẫu nằm trong `test-data/`. Có thể nhập tay cho từng bản ghi ở mọi tab import.

## Đặt lại dữ liệu

`scripts/reset/` chứa hai script xóa dữ liệu. Chỉ dùng khi đã sao lưu, và chạy theo thứ tự:

### Bước 1: MongoDB (tin nhắn, nhóm chat)

Chạy qua container `chat-db`, từ WSL tại thư mục chứa `reset-chat.js`:

1. Chạy thử. File đang để `DRY_RUN = true`, lệnh chỉ in số document, không xóa gì:
   ```bash
   docker exec -i chat-db mongosh linkwave --quiet < reset-chat.js
   ```
2. Khi số liệu đúng, mở file, đổi `const DRY_RUN = true;` thành `const DRY_RUN = false;`, lưu, rồi chạy lại lệnh trên.

Script drop 6 collection: `chats`, `messages`, `group_invitations`, `message_revisions`, `message_reports`, `users`. Redis không bị ảnh hưởng.

### Bước 2: PostgreSQL (tài khoản, học vụ)

Chạy trong TablePlus, kết nối `localhost`, cổng `15432`, user `postgres`, database `lw-users`, mật khẩu lấy từ `USERS_DB_PASSWORD` trong `backend/.env`.

1. Mở `reset-keep-root-and-roles.sql`, chạy phần kiểm tra trước (phần A).
2. Chạy toàn bộ file. File có `BEGIN;` ở đầu và **không** có `COMMIT;`, nên thay đổi chưa được ghi vào DB.
3. Xem kết quả phần C: `users = 1`, các bảng dữ liệu bằng 0, còn `roles`, `permissions`, `role_permissions` giữ nguyên.
4. Đúng thì chạy `COMMIT;`, sai thì chạy `ROLLBACK;`. Trong TablePlus, nếu có nút Commit thì bấm nút đó.

Script giữ lại hai tài khoản hệ thống root và sgu_bot, cùng toàn bộ roles, permissions và role_permissions. Các tài khoản người dùng khác, dữ liệu học vụ, hồ sơ cá nhân, danh bạ và dữ liệu audit sẽ được xóa.

Bảng flyway_schema_history và các migration đã thực thi được giữ nguyên. Bảng deactivated_tokens cũng được giữ lại để không làm mất trạng thái thu hồi access token.

Sau khi reset, database vẫn giữ nguyên schema và có thể tiếp tục hoạt động mà không cần chạy lại migration.

**Lưu ý:** Sao lưu DB trước khi chạy cả ba bước.

## Bảo mật và giới hạn đã biết

- CCCD, dân tộc và tôn giáo được mã hóa AES-GCM. Xem thông tin đầy đủ cần quyền và được ghi vào nhật ký.
- Mọi lệnh gọi nội bộ giữa service dùng `INTERNAL_SERVICE_KEY`.
- Khóa tài khoản: WebSocket đang mở bị ngắt ngay; phiên HTTP còn hiệu lực tối đa 5 phút.
- Số ghim (5 cuộc trò chuyện) kiểm tra theo từng yêu cầu, chưa khóa nguyên tử nên có thể vượt nếu bấm đồng thời.
- Tìm kiếm theo tên không phân biệt dấu cần kiểm thử với dữ liệu tên thật.

## Giấy phép

Xem file [LICENSE](LICENSE).