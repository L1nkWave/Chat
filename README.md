# SGU Chat

Ứng dụng chat nội bộ cho Đại học Sài Gòn, giao diện kiểu Zalo. Mã nguồn mở, tự host, không kết nối ra dịch vụ bên ngoài khi chạy.

## Kiến trúc

| Thành phần | Công nghệ | Cổng |
|---|---|---|
| frontend | Next.js 14 | 3000 |
| api-gateway | Spring Cloud Gateway | 8080 |
| discovery-service | Eureka | 8761 |
| auth-service | Spring Boot, JWT, PostgreSQL | 8081 |
| user-service | Spring Boot, PostgreSQL | 8082 |
| chat-service | Spring Boot, MongoDB | 8083 |
| ws-server | Spring Boot WebSocket, Redis | 9090 |
| PostgreSQL (users) | | 15432 |
| MongoDB (chat) | replica set `rs0` | 27017 |
| Redis (phiên WebSocket) | | 16379 |

Trình duyệt chỉ gọi frontend và api-gateway. Các service tự đăng ký với Eureka.

## Yêu cầu

- JDK 18 (không dùng JDK 26, Lombok 1.18.28 không tương thích)
- Node.js 18 trở lên
- Docker Desktop (để chạy PostgreSQL, MongoDB, Redis)
- Git

## 1. Lấy mã nguồn

```bash
git clone https://github.com/<tên-github>/SGU-Chat.git
cd SGU-Chat
```

## 2. Chạy hạ tầng (PostgreSQL, MongoDB, Redis)

Chạy riêng 3 container, không kéo theo các service khác:

```bash
cd backend
docker compose up -d --no-deps users-db chat-db redis-sessions
docker compose ps
```

Đợi đến khi các container ở trạng thái `healthy`. Sau đó khởi tạo replica set cho MongoDB (chỉ làm một lần):

```bash
docker exec chat-db mongosh --eval "rs.initiate()"
```

Nếu báo `already initialized` thì bỏ qua.

## 3. Cấu hình

### 3.1 Database

Mở `backend/auth-service/src/main/resources/application.yml` và `backend/user-service/src/main/resources/application.yml`, sửa phần `datasource`:

```yaml
  datasource:
    url: 'jdbc:postgresql://localhost:15432/lw-users'
    username: 'postgres'
    password: 'postgres'
```

Mật khẩu phải khớp với `USERS_DB_PASSWORD` trong `backend/.env`.

### 3.2 Redis

Mở `backend/ws-server/src/main/resources/application.yml`, sửa cổng Redis:

```yaml
      port: 16379
```

### 3.3 Thư mục lưu file

Mở `backend/chat-service/src/main/resources/application.yml`, đổi `files.storage-folder` thành một thư mục tồn tại trên máy, ví dụ:

```yaml
files:
  storage-folder: './storage'
```

### 3.4 Khóa RSA

Các file khóa trong repo chỉ là file rỗng, phải tự tạo khóa mới (cần `openssl`, có sẵn trong Git for Windows):

```bash
cd backend/auth-service/src/main/resources/keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out access_private_key.pem
openssl rsa -pubout -in access_private_key.pem -out access_public_key.pem
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out refresh_private_key.pem
openssl rsa -pubout -in refresh_private_key.pem -out refresh_public_key.pem
```

Sau đó sao chép `access_public_key.pem` sang các service còn lại (phải là cùng một khóa):

```bash
cp access_public_key.pem ../../../../user-service/src/main/resources/keys/
cp access_public_key.pem ../../../../chat-service/src/main/resources/keys/
cp access_public_key.pem ../../../../ws-server/src/main/resources/keys/
```

Trên Windows PowerShell, dùng `Copy-Item` thay cho `cp`. Nếu thư mục `keys` chưa tồn tại thì tạo trước.

### 3.5 Frontend

```bash
cd frontend
cp .env.example .env.local
```

## 4. Build thư viện chung

Chạy một lần từ `backend/auth-service` (Windows: `mvnw.cmd`):

```bash
./mvnw -f ../pom.xml install -N -DskipTests
./mvnw -f ../shared/pom.xml install -DskipTests
```

Nếu `mvnw` báo `Could not find or load main class MavenWrapperMain`, file `.mvn/wrapper/maven-wrapper.jar` bị lỗi. Sao chép thư mục `.mvn` từ `discovery-service` sang service đó.

## 5. Chạy backend

Mỗi service chạy trong một terminal riêng, **đúng thứ tự**. Chờ service trước lên hẳn (`Started ...Application`) rồi mới chạy service sau:

```bash
cd backend/discovery-service && ./mvnw spring-boot:run
cd backend/auth-service      && ./mvnw spring-boot:run
cd backend/user-service      && ./mvnw spring-boot:run
cd backend/chat-service      && ./mvnw spring-boot:run
cd backend/ws-server         && ./mvnw spring-boot:run
cd backend/api-gateway       && ./mvnw spring-boot:run
```

Kiểm tra http://localhost:8761: phải thấy `API-GATEWAY`, `AUTH-SERVICE`, `USER-SERVICE`, `CHAT-SERVICE`, `WS-SERVER` đều UP.

## 6. Chạy frontend

```bash
cd frontend
npm install
npx next dev -p 3000
```

Mở http://localhost:3000.

## 7. Kiểm tra nhanh

1. Đăng ký hai tài khoản, đăng nhập bằng từng tài khoản ở hai trình duyệt (một bình thường, một ẩn danh).
2. Tìm và thêm liên hệ, gửi tin nhắn chữ, gửi ảnh. Bên kia phải nhận được ngay không cần tải lại trang.
3. Thử gửi tin khi một bên đăng xuất, rồi đăng nhập lại để kiểm tra lịch sử.

## 8. Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| Đăng ký báo 404 | Thiếu `frontend/.env.local` | Tạo file theo mục 3.5, khởi động lại `npm run dev` |
| `WebSocket` lỗi `undefined` | Thiếu `NEXT_PUBLIC_WEB_SOCKET_URL` | Thêm biến vào `.env.local`, khởi động lại frontend |
| `Missing key encoding` | File khóa rỗng hoặc sai định dạng | Tạo lại khóa theo mục 3.4 |
| `Connection refused: localhost:5432` | Service dùng cổng mặc định | Sửa `application.yml` theo mục 3.1 |
| `password authentication failed` | Mật khẩu DB không khớp | Đồng bộ mật khẩu trong `application.yml` và container |
| `Could not find artifact org.linkwave:shared` | Chưa build thư viện chung | Chạy mục 4 |
| `Could not resolve ... backend:pom` | Chưa cài pom gốc | Chạy `install -N` ở mục 4 |
| Tin nhắn không đến bên kia | WebSocket bị đóng | Kiểm tra `ws-server` đã chạy và đăng ký với Eureka |
| CORS khi gọi API | Origin không có trong `ORIGINS` | Thêm địa chỉ frontend vào `ORIGINS` trong `backend/.env` |

## 9. Tình trạng và việc còn lại

Đã có: đăng ký, đăng nhập, danh bạ, chat 1-1, chat nhóm, gửi file và ảnh, đã đọc, trạng thái trực tuyến.

Chưa có:
- Cấp giảng viên (hiện chỉ có `USER` và `ADMIN`).
- Người được thêm vào nhóm phải đồng ý trước khi vào.
- Quyền bậc trên quản lý bậc dưới.

## Bảo mật

- Đổi mật khẩu PostgreSQL và tạo khóa RSA mới trước khi triển khai thật.
- Thêm `.env` và `.env.local` vào `.gitignore`, và xóa file `.env` đã commit khỏi git bằng `git rm --cached backend/.env`.
- Không dùng khóa trong repo cho môi trường thật.