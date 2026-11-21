# SGU Chat

Ứng dụng chat nội bộ cho Đại học Sài Gòn, giao diện kiểu Zalo. Hệ thống gồm frontend Next.js và backend Spring Boot theo kiến trúc microservice.

## Kiến trúc

| Thành phần | Công nghệ | Cổng host |
|---|---|---:|
| frontend | Next.js 16 | 3000 |
| api-gateway | Spring Cloud Gateway | 8080 |
| discovery-service | Eureka | 8761 |
| auth-service | Spring Boot, JWT, PostgreSQL | 8081 |
| user-service | Spring Boot, PostgreSQL | 8082 |
| chat-service | Spring Boot, MongoDB replica set | 8083 |
| ws-server | Spring Boot WebSocket, 2 instances E1/E2, Redis | 9090, 9091 |
| PostgreSQL | users database | 15432 |
| MongoDB | replica set `rs0` | 27017 |
| Redis | WebSocket sessions/broadcast | 16379 |

Trong Docker, các backend service giao tiếp qua DNS nội bộ của Compose (ví dụ `users-db`, `chat-db`, `redis-sessions`, `discovery-service`), không dùng `localhost` để gọi container khác. Trình duyệt gọi API qua `api-gateway`.

## Yêu cầu

- Docker Desktop có Docker Compose v2.
- Git.
- Node.js 20.9 trở lên và npm để chạy frontend.
- OpenSSL để tạo RSA key lần đầu (Git for Windows thường đã đi kèm OpenSSL).

Không cần cài JDK hoặc Maven để chạy backend bằng Docker. Dockerfile build backend bằng Java 21, đúng với `backend/pom.xml`.

## 1. Clone repository

```bash
git clone https://github.com/<github-username>/SGU-Chat.git
cd SGU-Chat
```

## 2. Cấu hình biến môi trường backend

Tạo `backend/.env` từ file mẫu:

```bash
cp backend/.env.example backend/.env
```

Trên PowerShell:

```powershell
Copy-Item backend/.env.example backend/.env
```

Mở `backend/.env`, thay `USERS_DB_PASSWORD` bằng mật khẩu riêng. `ORIGINS` mặc định là `http://localhost:3000`; thay đổi nếu frontend chạy ở origin khác.

## 3. Tạo RSA keys

Các key trong `src/main/resources/keys` là key theo môi trường chạy. Hãy tạo key mới trên máy local; không commit các file PEM này.

Chạy từ thư mục gốc repository (Bash/Git Bash):

```bash
cd backend/auth-service/src/main/resources/keys

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out access_private_key.pem
openssl pkey -pubout -in access_private_key.pem -out access_public_key.pem

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out refresh_private_key.pem
openssl pkey -pubout -in refresh_private_key.pem -out refresh_public_key.pem

cp access_public_key.pem ../../../../../user-service/src/main/resources/keys/
cp access_public_key.pem ../../../../../chat-service/src/main/resources/keys/
cp access_public_key.pem ../../../../../ws-server/src/main/resources/keys/
```

Trên PowerShell, thay ba lệnh `cp` cuối bằng `Copy-Item` và dùng cùng đường dẫn tương đối gồm năm cấp `..`. Nếu thư mục `keys` ở service đích chưa tồn tại, hãy tạo thư mục trước. Sau bước này, quay lại thư mục gốc repository trước khi tiếp tục.

**Quan trọng:** `access_public_key.pem` phải được sao chép từ đúng `access_private_key.pem` đang dùng để ký access token. Tương tự, refresh public key phải được tạo từ refresh private key tương ứng. Không sao chép refresh key sang các service khác.

## 4. Khởi động toàn bộ backend bằng Docker Compose

Chạy từ thư mục `backend`:

```bash
cd backend
docker compose up --build -d
```

Lệnh này build các image backend từ Maven multi-module project, sau đó khởi động:

- 6 loại application: API Gateway, Discovery, Auth, User, Chat và WebSocket.
- 2 container WebSocket (E1 và E2), dùng chung Redis để broadcast giữa các instance.
- PostgreSQL, MongoDB replica set `rs0` và Redis.

Kiểm tra trạng thái:

```bash
docker compose ps
```

Theo dõi log:

```bash
docker compose logs -f
```

Hoặc xem log một service:

```bash
docker compose logs -f auth-service
```

Khi hoàn tất, Eureka Dashboard tại http://localhost:8761 phải hiển thị `API-GATEWAY`, `AUTH-SERVICE`, `USER-SERVICE`, `CHAT-SERVICE` và `WS-SERVER` là `UP`. `WS-SERVER` có hai instance.

Các lần chạy sau, nếu không thay đổi mã nguồn backend, có thể dùng:

```bash
docker compose up -d
```

## 5. Chạy frontend

Mở terminal khác, từ thư mục gốc repository:

```bash
cd frontend
cp .env.example .env.local
npm ci
npm run dev
```

Trên PowerShell, dùng `Copy-Item .env.example .env.local` thay cho `cp`.

Mở http://localhost:3000. Frontend gọi API và WebSocket thông qua API Gateway tại cổng 8080.

## 6. Một số lệnh Docker hữu ích

```bash
# Xem trạng thái các container
docker compose ps

# Xem log tất cả service
docker compose logs -f

# Khởi động lại một service
docker compose restart auth-service

# Dừng container nhưng giữ dữ liệu trong named volumes
docker compose down

# Xóa cả dữ liệu PostgreSQL, MongoDB, Redis và media storage
# CẢNH BÁO: thao tác này làm mất dữ liệu đã lưu.
docker compose down -v
```

MongoDB replica set được khởi tạo tự động bởi service `mongo-rs-init`; không cần chạy `rs.initiate()` thủ công. PostgreSQL, MongoDB, Redis và media storage dùng named volumes để dữ liệu không mất khi chỉ chạy `docker compose down`.

## 7. Bảo mật và Git

- `backend/.env`, `frontend/.env.local` và key PEM trong `src/main/resources/keys` không được commit.
- `.gitignore` không tự ngừng theo dõi file đã được commit trước đó. Để bỏ các PEM production khỏi Git index nhưng vẫn giữ file trên máy:

  ```bash
  git rm --cached -- ':(glob)backend/**/src/main/resources/keys/*.pem'
  git commit -m "Stop tracking environment-specific RSA keys"
  ```

- Nếu private key đã từng được push lên remote, hãy tạo cặp key mới và thay key ở tất cả service. Xóa file ở commit mới không làm mất nội dung key trong lịch sử Git; nếu repository công khai, cần cân nhắc làm sạch lịch sử Git sau khi đã rotate key.
- Các key trong `src/test/resources` chỉ dùng cho test, không được tái sử dụng cho môi trường chạy thật.
- Không sử dụng mật khẩu mẫu hoặc key phát triển khi triển khai production.

## 8. Lỗi thường gặp

| Triệu chứng | Nguyên nhân thường gặp | Cách kiểm tra |
|---|---|---|
| Container backend thoát ngay khi start | Thiếu PEM hoặc PEM sai định dạng | `docker compose logs auth-service` và kiểm tra mục 3 |
| `password authentication failed` | Mật khẩu trong `.env` không khớp hoặc volume DB cũ | Kiểm tra `DB_USER`, `USERS_DB_PASSWORD`; không xóa volume nếu cần giữ dữ liệu |
| Mongo báo replica set chưa khởi tạo | `mongo-rs-init` chưa hoàn tất | `docker compose logs mongo-rs-init chat-db` |
| Service không xuất hiện trên Eureka | Sai `defaultZone` hoặc service chưa healthy | Kiểm tra log `discovery-service` và service tương ứng |
| API trả về 503 từ Gateway | Gateway chưa thấy service đích trên Eureka | Kiểm tra service đích và Eureka Dashboard |
| WebSocket không kết nối | Sai `NEXT_PUBLIC_WS_URL` hoặc gateway chưa thấy `ws-server` | Dùng `ws://localhost:8080/ws`, kiểm tra `api-gateway` và cả hai WS instance |
| Cổng host đã được sử dụng | Một tiến trình khác đang dùng cổng 8080/8761/15432/27017/16379 | Đổi port host tương ứng trong `backend/.env` |

## 9. Tình trạng và việc còn lại

Đã có: đăng ký, đăng nhập, danh bạ, chat 1-1, chat nhóm, gửi file và ảnh, đã đọc, trạng thái trực tuyến.

Chưa có:
- Cấp giảng viên (hiện chỉ có `USER` và `ADMIN`).
- Người được thêm vào nhóm phải đồng ý trước khi vào.
- Quyền bậc trên quản lý bậc dưới.
