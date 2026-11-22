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
| user-service | Spring Boot, JPA, PostgreSQL, Apache POI (import Excel) | 8082 |
| chat-service | Spring Boot, MongoDB, đồng bộ nhóm học vụ | 8083 |
| ws-server | Spring Boot WebSocket, nhận sự kiện từ chat-service (chỉ chạy một instance) | 9090 |
| discovery-service | Eureka | 8761 |
| users-db | PostgreSQL 16 (CSDL `lw-users`) | 15432 |
| chat-db | MongoDB 7 (replica set `rs0`) | 27017 |
| redis-sessions | Redis | 16379 |

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
docker compose up -d --no-deps users-db redis-sessions discovery-service chat-db mongo-init
```

`--no-deps` bắt buộc ở đây: `chat-db` trong compose phụ thuộc auth-service, nếu không có cờ này compose sẽ kéo cả auth và user lên và chiếm cổng 8081, 8082 trước khi chạy Maven. Service `mongo-init` khởi tạo replica set `rs0` sau khi `chat-db` sẵn sàng. Kiểm tra bằng `docker compose logs mongo-init`.

### 3. Chạy backend

Có hai cách.

**Cách A, toàn bộ bằng Docker:**

```bash
cd backend
docker compose up -d --build
```

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

Kiểm tra: `http://localhost:8761` (Eureka) phải thấy đủ các service đã đăng ký.

### 4. Chạy frontend

```bash
cd frontend
npm ci
npm run dev        # http://localhost:3000
```

Tài khoản quản trị gốc (root) lấy từ người phụ trách dự án. Đổi mật khẩu ngay sau lần đăng nhập đầu tiên.

## Khởi tạo dữ liệu nền (chỉ làm khi DB mới)

Thư mục `backend/user-service/src/main/resources/db/` chứa các file SQL bổ sung bảng, vai trò, tài khoản bot và hồ sơ người dùng. Docker không tự chạy các file này, nên phải chạy tay, đúng thứ tự, trên DB `lw-users`.

1. Chạy user-service một lần (bằng `docker compose` hoặc `mvn spring-boot:run`) để Hibernate tạo các bảng `users`, `roles`. Đợi service lên rồi dừng lại.
2. Trong WSL, tại thư mục gốc repo, chạy:

```bash
for f in phase2-bot phase3-p1-roles phase4-admin phase5-academic phase5b-academic-fix phase6-subjects phase7-user-profiles phase7b-role-display; do
  echo "== $f"
  docker exec -i users-db psql -v ON_ERROR_STOP=1 -U postgres -d lw-users < backend/user-service/src/main/resources/db/$f.sql || break
done
```

`ON_ERROR_STOP=1` dừng ngay ở lỗi đầu tiên để không chạy tiếp trên dữ liệu lỗi.

3. Khởi động lại user-service.
4. Lấy ID của `sgu_bot` và đặt `BOT_USER_ID` trong `backend/.env`, kiểm tra bảng user trong TablePlus hay phần mềm quản trị database (mặc định là 1). Trong bất cứ trường hợp nào ảnh hưởng đến dữ liệu database hay không xuất hiện đoạn chat với bot. Vui lòng xem bước 3 trong phần "Đặt lại dữ liệu" bên dưới.

### Tạo tài khoản root

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

Sau khi import, bấm **Đồng bộ nhóm học vụ**. Nhóm lớp, môn, khoa được tạo từ dữ liệu này. Đồng bộ là thao tác thủ công của admin; chạy lại không tạo nhóm trùng, nhưng lời mời đã từ chối sẽ được gửi lại.

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

Script giữ lại tài khoản `root`, bảng `roles`, `permissions`, `role_permissions`. Script xóa cả tài khoản bot `sgu_bot` (nằm trong `users`).

### Bước 3: Tạo lại bot sau khi reset

Sau khi `COMMIT` ở bước 2, bot phải được tạo lại và chat-service phải biết ID mới của bot. Nếu bỏ qua, chat của bot sẽ không hiện, dù tài khoản đã tồn tại.

1. Chạy lại `phase2-bot.sql` trên DB `lw-users`:
   ```bash
   docker exec -i users-db psql -v ON_ERROR_STOP=1 -U postgres -d lw-users < backend/user-service/src/main/resources/db/phase2-bot.sql
   ```
2. Lấy ID của bot:
   ```bash
   docker exec -i users-db psql -U postgres -d lw-users -c "select id, username from users where username = 'sgu_bot';"
   ```
3. Mở `backend/.env`, thêm hoặc sửa dòng `BOT_USER_ID=<id vừa lấy>`.
4. Tạo lại container chat-service để đọc giá trị mới. `restart` không đủ, vì `env_file` chỉ được đọc khi tạo container:
   ```bash
   cd backend
   docker compose up -d --no-deps --force-recreate chat-service
   ```
5. Refresh trang và kiểm tra chat của bot.

ID thay đổi mỗi lần tạo lại `sgu_bot`, nên phải làm lại bước 2 đến 4 mỗi lần reset.

Sao lưu DB trước khi chạy cả ba bước.

## Bảo mật và giới hạn đã biết

- CCCD, dân tộc và tôn giáo được mã hóa AES-GCM. Xem thông tin đầy đủ cần quyền và được ghi vào nhật ký.
- Mọi lệnh gọi nội bộ giữa service dùng `INTERNAL_SERVICE_KEY`.
- Khóa tài khoản: WebSocket đang mở bị ngắt ngay; phiên HTTP còn hiệu lực tối đa 5 phút.
- Số ghim (5 cuộc trò chuyện) kiểm tra theo từng yêu cầu, chưa khóa nguyên tử nên có thể vượt nếu bấm đồng thời.
- Tìm kiếm theo tên không phân biệt dấu cần kiểm thử với dữ liệu tên thật.

## Giấy phép

Xem file [LICENSE](LICENSE).