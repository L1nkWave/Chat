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
| ws-server | Spring Boot WebSocket, nhận sự kiện từ chat-service | 9090 |
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
  scripts/      Khóa và công cụ phụ trợ
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
- `openssl` để sinh khóa

## Cài đặt

### 1. Sinh khóa và tạo file môi trường

```bash
bash backend/scripts/gen-keys.sh   # chạy từ thư mục gốc; sinh cặp khóa RSA cho auth-service
cp backend/.env.example backend/.env
cp frontend/env.example frontend/.env.local
```

`gen-keys.sh` chỉ sinh khóa RSA (access và refresh token). Các giá trị còn lại trong `backend/.env` tự tạo bằng `openssl rand -base64 32`:

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

## Kiểm thử

Backend:

```bash
cd backend/user-service && mvn test
cd backend/chat-service && mvn test
```

Frontend:

```bash
cd frontend
npm run typecheck
npm run lint
npm test
npm run build
```

Kiểm tra thủ công nên làm: import đủ 9 file, đồng bộ nhóm, nhắn tin giữa hai trình duyệt (realtime), mở lại chat riêng đã ẩn, tải và xóa tệp, giao diện trên điện thoại (390px).

## Đặt lại dữ liệu

`scripts/reset/` chứa hai script xóa dữ liệu. Chỉ dùng khi đã sao lưu, và chạy theo thứ tự:

- `reset-keep-root-and-roles.sql`: xóa dữ liệu PostgreSQL, giữ tài khoản root và các vai trò, quyền cơ bản.
- `reset-chat.js`: xóa tin nhắn và nhóm trong MongoDB. Mặc định `DRY_RUN=true` (chỉ in, không xóa). Đặt `DRY_RUN=false` để thực sự xóa.

Đọc kỹ script trước khi chạy. Sau khi reset, nhập lại dữ liệu theo thứ tự ở trên.

## Bảo mật và giới hạn đã biết

- CCCD, dân tộc và tôn giáo được mã hóa AES-GCM. Xem thông tin đầy đủ cần quyền và được ghi vào nhật ký.
- Mọi lệnh gọi nội bộ giữa service dùng `INTERNAL_SERVICE_KEY`.
- Khóa tài khoản: WebSocket đang mở bị ngắt ngay; phiên HTTP còn hiệu lực tối đa 5 phút.
- Số ghim (5 cuộc trò chuyện) kiểm tra theo từng yêu cầu, chưa khóa nguyên tử nên có thể vượt nếu bấm đồng thời.
- Tìm kiếm theo tên không phân biệt dấu cần kiểm thử với dữ liệu tên thật.

## Giấy phép

Xem file [LICENSE](LICENSE).