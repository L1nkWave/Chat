// =====================================================================
// reset-chat.js - Xóa dữ liệu chat trong MongoDB (database: linkwave, replica set rs0)
//
// TRẠNG THÁI: CHỈ LÀ FILE. Chưa chạy.
// Chạy bằng mongosh (sau khi đã đặt DRY_RUN = false):
//   mongosh "mongodb://localhost:27017/linkwave?replicaSet=rs0" reset-chat.js
//
// DRY_RUN = true  : chỉ in số document của từng collection, KHÔNG xóa gì.
// DRY_RUN = false : xóa (drop) các collection liệt kê trong DROP_COLLECTIONS.
//
// Tên collection lấy từ @Document trong backend/chat-service:
//   chats               Chat, GroupChat            (chat/duo, chat/group)
//   messages            Message và các lớp con     (message/*)
//   group_invitations   GroupInvitation            (invitation)
//   message_revisions   MessageRevision            (revision)
//   message_reports     MessageReport              (moderation/report)
//   users               User (userId, con trỏ đọc tin nhắn phía chat)
//
// Không xóa: các collection khác của database (nếu có) và mọi dữ liệu ngoài MongoDB.
// Redis (cổng 16379) không liên quan và không được chạm tới ở đây.
// =====================================================================

const DRY_RUN = true;                    // ĐẶT false ĐỂ XÓA THẬT
const DB_NAME = "linkwave";
const DROP_COLLECTIONS = [
  "chats",
  "messages",
  "group_invitations",
  "message_revisions",
  "message_reports",
  "users",
];

const target = db.getSiblingDB(DB_NAME);
const existing = new Set(target.getCollectionNames());

print(`Database: ${DB_NAME} | DRY_RUN = ${DRY_RUN}`);
print("Collection                 truoc");
for (const name of DROP_COLLECTIONS) {
  const count = existing.has(name) ? target.getCollection(name).countDocuments({}) : "(khong ton tai)";
  print(`${name.padEnd(26)} ${count}`);
}

if (DRY_RUN) {
  print("DRY_RUN = true: chua xoa gi. Dat DRY_RUN = false de thuc hien.");
} else {
  for (const name of DROP_COLLECTIONS) {
    if (!existing.has(name)) {
      print(`Bo qua ${name} (khong ton tai)`);
      continue;
    }
    const ok = target.getCollection(name).drop();
    print(`Da drop ${name}: ${ok}`);
  }
  print("Sau khi xoa:");
  const remaining = new Set(target.getCollectionNames());
  for (const name of DROP_COLLECTIONS) {
    print(`${name.padEnd(26)} ${remaining.has(name) ? "CON TON TAI" : "da xoa"}`);
  }
}
