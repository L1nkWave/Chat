// Mock backend để test giao diện: HTTP (cổng 8080) và WebSocket (cổng 9090).
// Chỉ dùng khi phát triển, không deploy. Chạy: node scripts/mock-server.mjs
// Cần: npm install -D ws@8.18.0
import http from "node:http";
import { randomBytes } from "node:crypto";
import { WebSocketServer } from "ws";

const HTTP_PORT = 8080;
const WS_PORT = 9090;
const TICKET_TTL_MS = 30_000;
const MAX_FILE_BYTES = 10 * 1024 * 1024;
const MAX_TITLE = 64;

// Tài khoản test, mật khẩu chung: Test1234
const users = new Map([
    ["sv1", { password: "Test1234", name: "Sinh viên 1" }],
    ["sv2", { password: "Test1234", name: "Sinh viên 2" }],
    ["sv3", { password: "Test1234", name: "Sinh viên 3" }],
    ["gv1", { password: "Test1234", name: "Giảng viên 1" }],
]);

// Nhóm có sẵn: g1 (sv1 làm chủ, có sv2), g2 (gv1 làm chủ, có sv3).
// Trạng thái nằm trong bộ nhớ, khởi động lại là về mặc định.
const groups = new Map([
    [
        "g1",
        {
            id: "g1",
            title: "Nhóm đồ án mẫu",
            ownerId: "sv1",
            members: new Set(["sv1", "sv2"]),
            messages: [
                {
                    id: "seed1",
                    conversationId: "g1",
                    senderId: "sv2",
                    senderName: "Sinh viên 2",
                    text: "Chào mọi người.",
                    time: "08:00",
                    attachment: null,
                },
            ],
        },
    ],
    [
        "g2",
        {
            id: "g2",
            title: "Câu lạc bộ lập trình",
            ownerId: "gv1",
            members: new Set(["gv1", "sv3"]),
            messages: [],
        },
    ],
]);

// id -> { id, groupId, invitee, inviter }
const invitations = new Map();

// Mock chỉ kiểm tra magic bytes cho các định dạng dưới đây. Server thật phải kiểm tra tất cả.
const SIGNATURES = [
    { mime: "image/png", test: (b) => b.subarray(0, 4).toString("hex") === "89504e47" },
    { mime: "image/jpeg", test: (b) => b.subarray(0, 3).toString("hex") === "ffd8ff" },
    { mime: "image/gif", test: (b) => b.subarray(0, 4).toString("ascii") === "GIF8" },
    { mime: "application/pdf", test: (b) => b.subarray(0, 4).toString("ascii") === "%PDF" },
];

const ALLOWED_MIME = new Set([
    "image/png",
    "image/jpeg",
    "image/gif",
    "image/webp",
    "application/pdf",
    "text/plain",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
]);

// Chỉ dùng để test: tài khoản nào bị chặn kết nối WebSocket đến thời điểm nào.
const blockedUntil = new Map();

const newId = () => randomBytes(8).toString("hex");
const nameOf = (username) => users.get(username)?.name ?? username;

const refreshCookie = (username) =>
    `Refresh-Token=mock-refresh.${username}; Path=/api/v1/auth/refresh-tokens; HttpOnly; SameSite=Strict; Max-Age=3600`;

const issueTokens = (username) => ({
    accessToken: `mock.${username}.${Date.now()}`,
    refreshExpiration: Date.now() + 60 * 60 * 1000,
});

function usernameFromBearer(req) {
    const match = /^Bearer mock\.([^.]+)\./.exec(req.headers.authorization ?? "");
    return match && users.has(match[1]) ? match[1] : null;
}

function usernameFromRefreshCookie(req) {
    const match = /Refresh-Token=mock-refresh\.([^;\s]+)/.exec(req.headers.cookie ?? "");
    return match && users.has(match[1]) ? match[1] : null;
}

function send(res, status, body, headers = {}) {
    const hasBody = body !== undefined;
    res.writeHead(status, {
        ...(hasBody ? { "Content-Type": "application/json; charset=utf-8" } : {}),
        ...headers,
    });
    res.end(hasBody ? JSON.stringify(body) : undefined);
}

function readJson(req) {
    return new Promise((resolve) => {
        let data = "";
        req.on("data", (chunk) => (data += chunk));
        req.on("end", () => {
            try {
                resolve(data ? JSON.parse(data) : {});
            } catch {
                resolve({});
            }
        });
    });
}

// Đọc body nhị phân, từ chối nếu vượt giới hạn.
function readRaw(req, limit) {
    return new Promise((resolve, reject) => {
        const chunks = [];
        let total = 0;
        let tooLarge = false;
        req.on("data", (chunk) => {
            total += chunk.length;
            if (total > limit) {
                tooLarge = true;
                return;
            }
            chunks.push(chunk);
        });
        req.on("end", () =>
            tooLarge ? reject(new Error("TOO_LARGE")) : resolve(Buffer.concat(chunks)),
        );
        req.on("error", reject);
    });
}

function sanitizeName(name) {
    const base = name.split(/[\\/]/).pop() ?? "";
    const cleaned = base.replace(/[\x00-\x1f"<>|?*]/g, "_").slice(0, 120);
    return cleaned || "tep";
}

// Ticket dùng một lần, hết hạn sau 30 giây.
const tickets = new Map();

function issueTicket(username) {
    const ticket = randomBytes(24).toString("hex");
    tickets.set(ticket, { username, expires: Date.now() + TICKET_TTL_MS });
    return ticket;
}

function consumeTicket(ticket) {
    const entry = tickets.get(ticket);
    tickets.delete(ticket);
    if (!entry || entry.expires < Date.now()) return null;
    return entry.username;
}

// Tệp đã tải lên: id -> { owner, buffer, contentType, filename }
const files = new Map();

async function uploadFileRoute(req, res, url) {
    const username = usernameFromBearer(req);
    if (!username) return send(res, 401, { message: "Chưa đăng nhập" });

    let buffer;
    try {
        buffer = await readRaw(req, MAX_FILE_BYTES);
    } catch {
        return send(res, 413, { message: "Tệp vượt quá 10 MB." });
    }

    const declared = String(req.headers["content-type"] ?? "");
    if (buffer.length === 0) return send(res, 400, { message: "Tệp rỗng." });
    if (!ALLOWED_MIME.has(declared)) {
        return send(res, 415, { message: "Định dạng tệp không được hỗ trợ." });
    }
    const rule = SIGNATURES.find((s) => s.mime === declared);
    if (rule && !rule.test(buffer)) {
        return send(res, 415, { message: "Nội dung tệp không khớp định dạng khai báo." });
    }

    const id = randomBytes(16).toString("hex");
    const filename = sanitizeName(url.searchParams.get("filename") ?? "");
    files.set(id, { owner: username, buffer, contentType: declared, filename });
    return send(res, 201, { id, filename, contentType: declared, size: buffer.length });
}

function downloadFileRoute(req, res, id) {
    const username = usernameFromBearer(req);
    if (!username) return send(res, 401, { message: "Chưa đăng nhập" });
    const file = files.get(id);
    if (!file) return send(res, 404, { message: "Không tìm thấy tệp" });

    const inline = file.contentType.startsWith("image/");
    res.writeHead(200, {
        "Content-Type": file.contentType,
        "Content-Length": file.buffer.length,
        "X-Content-Type-Options": "nosniff",
        "Content-Disposition": `${inline ? "inline" : "attachment"}; filename*=UTF-8''${encodeURIComponent(file.filename)}`,
        "Cache-Control": "private, no-store",
    });
    res.end(file.buffer);
}

// Chỉ dùng để test. ?user=sv1 chỉ ngắt sv1; bỏ trống thì ngắt tất cả.
// block=N chặn tài khoản đó kết nối WebSocket lại trong N giây.
function dropSocketsRoute(res, url) {
    const seconds = Math.max(0, Number(url.searchParams.get("block") ?? 0));
    const user = url.searchParams.get("user");
    const until = Date.now() + seconds * 1000;
    let dropped = 0;
    for (const c of clients) {
        if (user && c.userId !== user) continue;
        blockedUntil.set(c.userId, until);
        c.socket.terminate();
        dropped += 1;
    }
    if (user) blockedUntil.set(user, until);
    send(res, 200, { dropped, user: user ?? "*", blockedSeconds: seconds });
}

// ----- Nhóm, thành viên, lời mời -----

function groupSummary(g) {
    const last = g.messages.at(-1);
    return {
        id: g.id,
        title: g.title,
        kind: "group",
        lastMessage: last ? last.text || `📎 ${last.attachment?.filename ?? ""}` : "",
        lastTime: last?.time ?? "",
        unread: 0,
    };
}

// Trả về nhóm nếu người gọi là thành viên, ngược lại gửi lỗi và trả null.
function requireMember(res, me, groupId) {
    const g = groups.get(groupId);
    if (!g) {
        send(res, 404, { message: "Không tìm thấy nhóm" });
        return null;
    }
    if (!g.members.has(me)) {
        send(res, 403, { message: "Bạn không phải thành viên nhóm này" });
        return null;
    }
    return g;
}

function notifyUser(username, event) {
    for (const c of clients) if (c.userId === username) emit(c.socket, "/chat", event);
}

function notifyGroup(g, event) {
    for (const c of clients) if (g.members.has(c.userId)) emit(c.socket, "/chat", event);
}

function listConversations(res, me) {
    const list = [...groups.values()].filter((g) => g.members.has(me)).map(groupSummary);
    send(res, 200, list);
}

function listMessages(res, me, [groupId]) {
    const g = requireMember(res, me, groupId);
    if (!g) return;
    send(res, 200, g.messages.slice(-100));
}

function listMembers(res, me, [groupId]) {
    const g = requireMember(res, me, groupId);
    if (!g) return;
    send(
        res,
        200,
        [...g.members].map((u) => ({ userId: u, name: nameOf(u), isOwner: u === g.ownerId })),
    );
}

function createGroup(res, me, _params, body) {
    const title = typeof body.title === "string" ? body.title.trim() : "";
    if (title.length < 3 || title.length > MAX_TITLE) {
        return send(res, 400, { message: "Tên nhóm cần từ 3 đến 64 ký tự." });
    }
    const g = { id: newId(), title, ownerId: me, members: new Set([me]), messages: [] };
    groups.set(g.id, g);
    send(res, 201, groupSummary(g));
}

// Chỉ chủ nhóm được đổi tên. Mọi thành viên nhận sự kiện để làm mới danh sách.
function renameGroup(res, me, [groupId], body) {
    const g = requireMember(res, me, groupId);
    if (!g) return;
    if (g.ownerId !== me) {
        return send(res, 403, { message: "Chỉ chủ nhóm mới được đổi tên nhóm." });
    }
    const title = typeof body.title === "string" ? body.title.trim() : "";
    if (title.length < 3 || title.length > MAX_TITLE) {
        return send(res, 400, { message: "Tên nhóm cần từ 3 đến 64 ký tự." });
    }
    g.title = title;
    notifyGroup(g, { type: "membership", conversationId: g.id });
    send(res, 200, groupSummary(g));
}

// Mọi thành viên đều mời được, nhưng người được mời phải chấp nhận.
function inviteToGroup(res, me, [groupId], body) {
    const g = requireMember(res, me, groupId);
    if (!g) return;
    const target = typeof body.username === "string" ? body.username.trim() : "";
    if (!users.has(target)) {
        return send(res, 404, { message: "Không tìm thấy người dùng này." });
    }
    if (g.members.has(target)) {
        return send(res, 409, { message: "Người này đã là thành viên của nhóm." });
    }
    const pending = [...invitations.values()].some(
        (i) => i.groupId === groupId && i.invitee === target,
    );
    if (pending) return send(res, 409, { message: "Đã gửi lời mời cho người này rồi." });

    const inv = { id: newId(), groupId, invitee: target, inviter: me };
    invitations.set(inv.id, inv);
    notifyUser(target, { type: "invitation" });
    send(res, 201, { id: inv.id });
}

// Chỉ chủ nhóm được xóa thành viên. Chủ nhóm không thể bị xóa.
function removeMember(res, me, [groupId, target]) {
    const g = requireMember(res, me, groupId);
    if (!g) return;
    if (g.ownerId !== me) {
        return send(res, 403, { message: "Chỉ chủ nhóm mới được xóa thành viên." });
    }
    if (target === g.ownerId) {
        return send(res, 409, { message: "Không thể xóa chủ nhóm." });
    }
    if (!g.members.has(target)) {
        return send(res, 404, { message: "Người này không có trong nhóm." });
    }
    g.members.delete(target);
    notifyGroup(g, { type: "membership", conversationId: g.id });
    // Người bị xóa không còn trong g.members nên gửi riêng.
    notifyUser(target, { type: "membership", conversationId: g.id });
    send(res, 204);
}

function listInvitations(res, me) {
    const list = [...invitations.values()]
        .filter((i) => i.invitee === me)
        .map((i) => ({
            id: i.id,
            groupId: i.groupId,
            groupTitle: groups.get(i.groupId)?.title ?? "",
            inviter: nameOf(i.inviter),
        }));
    send(res, 200, list);
}

function acceptInvitation(res, me, [id]) {
    const inv = invitations.get(id);
    if (!inv || inv.invitee !== me) {
        return send(res, 404, { message: "Không tìm thấy lời mời." });
    }
    invitations.delete(id);
    const g = groups.get(inv.groupId);
    if (!g) return send(res, 404, { message: "Nhóm không còn tồn tại." });
    g.members.add(me);
    // Người vừa được thêm đã nằm trong g.members nên cũng nhận được sự kiện.
    notifyGroup(g, { type: "membership", conversationId: g.id });
    send(res, 204);
}

function declineInvitation(res, me, [id]) {
    const inv = invitations.get(id);
    if (!inv || inv.invitee !== me) {
        return send(res, 404, { message: "Không tìm thấy lời mời." });
    }
    invitations.delete(id);
    send(res, 204);
}

function leaveGroup(res, me, [groupId]) {
    const g = requireMember(res, me, groupId);
    if (!g) return;
    if (g.ownerId === me) {
        return send(res, 409, { message: "Chủ nhóm không thể rời nhóm." });
    }
    g.members.delete(me);
    notifyGroup(g, { type: "membership", conversationId: g.id });
    // Người vừa rời không còn trong g.members nên gửi riêng.
    notifyUser(me, { type: "membership", conversationId: g.id });
    send(res, 204);
}

const routes = [
    { method: "GET", re: /^\/api\/v1\/chat\/conversations$/, handler: listConversations },
    { method: "GET", re: /^\/api\/v1\/chat\/conversations\/([a-z0-9]+)\/messages$/, handler: listMessages },
    { method: "GET", re: /^\/api\/v1\/chat\/conversations\/([a-z0-9]+)\/members$/, handler: listMembers },
    { method: "POST", re: /^\/api\/v1\/chat\/groups$/, handler: createGroup },
    { method: "PATCH", re: /^\/api\/v1\/chat\/groups\/([a-z0-9]+)$/, handler: renameGroup },
    { method: "POST", re: /^\/api\/v1\/chat\/groups\/([a-z0-9]+)\/invitations$/, handler: inviteToGroup },
    { method: "DELETE", re: /^\/api\/v1\/chat\/groups\/([a-z0-9]+)\/members\/([a-z0-9]+)$/, handler: removeMember },
    { method: "POST", re: /^\/api\/v1\/chat\/groups\/([a-z0-9]+)\/leave$/, handler: leaveGroup },
    { method: "GET", re: /^\/api\/v1\/chat\/invitations$/, handler: listInvitations },
    { method: "POST", re: /^\/api\/v1\/chat\/invitations\/([a-z0-9]+)\/accept$/, handler: acceptInvitation },
    { method: "POST", re: /^\/api\/v1\/chat\/invitations\/([a-z0-9]+)\/decline$/, handler: declineInvitation },
];

// ----- HTTP -----

const httpServer = http.createServer(async (req, res) => {
    const url = new URL(req.url, `http://localhost:${HTTP_PORT}`);
    const { pathname } = url;

    // Route tệp đọc body theo cách khác, nên xử lý trước khi đọc JSON.
    if (req.method === "POST" && pathname === "/api/v1/chat/files") {
        return uploadFileRoute(req, res, url);
    }
    const fileMatch = /^\/api\/v1\/chat\/files\/([a-f0-9]{32})$/.exec(pathname);
    if (req.method === "GET" && fileMatch) {
        return downloadFileRoute(req, res, fileMatch[1]);
    }
    if (req.method === "POST" && pathname === "/__mock/drop-sockets") {
        return dropSocketsRoute(res, url);
    }

    const body = req.method === "POST" || req.method === "PATCH" ? await readJson(req) : {};

    if (req.method === "POST" && pathname === "/api/v1/auth/login") {
        const user = users.get(body.username);
        if (!user || user.password !== body.password) {
            return send(res, 401, { message: "Sai tên đăng nhập hoặc mật khẩu" });
        }
        return send(res, 200, issueTokens(body.username), {
            "Set-Cookie": refreshCookie(body.username),
        });
    }

    if (req.method === "POST" && pathname === "/api/v1/auth/refresh-tokens") {
        const username = usernameFromRefreshCookie(req);
        if (!username) return send(res, 401, { message: "Phiên đăng nhập đã hết hạn" });
        return send(res, 200, issueTokens(username), {
            "Set-Cookie": refreshCookie(username),
        });
    }

    if (req.method === "POST" && pathname === "/api/v1/auth/logout") {
        return send(res, 204, undefined, {
            "Set-Cookie": "Refresh-Token=; Path=/api/v1/auth/refresh-tokens; HttpOnly; Max-Age=0",
        });
    }

    if (req.method === "POST" && pathname === "/api/v1/users/register") {
        if (!body.username || users.has(body.username)) {
            return send(res, 409, { message: "Tên đăng nhập đã tồn tại" });
        }
        users.set(body.username, {
            password: body.password,
            name: body.name ?? body.username,
        });
        return send(res, 201, undefined);
    }

    if (req.method === "POST" && pathname === "/api/v1/ws-tickets") {
        const username = usernameFromBearer(req);
        if (!username) return send(res, 401, { message: "Chưa đăng nhập" });
        return send(res, 201, { ticket: issueTicket(username) });
    }

    for (const route of routes) {
        if (req.method !== route.method) continue;
        const match = route.re.exec(pathname);
        if (!match) continue;
        const me = usernameFromBearer(req);
        if (!me) return send(res, 401, { message: "Chưa đăng nhập" });
        return route.handler(res, me, match.slice(1), body);
    }

    send(res, 404, { message: "Không tìm thấy" });
});

// ----- WebSocket -----

const clients = new Set();

function encodeFrame(path, payload) {
    return `path=${path}\n\n${JSON.stringify(payload)}`;
}

function decodeFrame(raw) {
    const text = raw.replace(/\r\n/g, "\n");
    const idx = text.indexOf("\n\n");
    if (idx < 0) return null;
    const head = text.slice(0, idx).trim();
    if (!head.startsWith("path=")) return null;
    try {
        return { path: head.slice(5).trim(), payload: JSON.parse(text.slice(idx + 2)) };
    } catch {
        return null;
    }
}

function emit(socket, path, payload) {
    if (socket.readyState === 1) socket.send(encodeFrame(path, payload));
}

function hhmm() {
    const now = new Date();
    return `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;
}

function handleFrame(client, raw) {
    const incoming = decodeFrame(raw);
    if (!incoming) {
        return emit(client.socket, "/error", { code: "BAD_FRAME", message: "Khung tin không hợp lệ" });
    }
    const { path, payload } = incoming;

    if (path === "/chat" && payload?.type === "send") {
        const text = typeof payload.text === "string" ? payload.text.trim() : "";
        const conversationId = typeof payload.conversationId === "string" ? payload.conversationId : "";
        const attachmentId = typeof payload.attachmentId === "string" ? payload.attachmentId : null;

        if (!conversationId || text.length > 2000 || (!text && !attachmentId)) {
            return emit(client.socket, "/error", { code: "BAD_PAYLOAD", message: "Tin nhắn không hợp lệ" });
        }

        // Kiểm tra quyền thành viên ở phía server, không tin vào giao diện.
        const group = groups.get(conversationId);
        if (!group) {
            return emit(client.socket, "/error", { code: "NOT_FOUND", message: "Không tìm thấy nhóm" });
        }
        if (!group.members.has(client.userId)) {
            return emit(client.socket, "/error", { code: "NOT_MEMBER", message: "Bạn không phải thành viên nhóm này" });
        }

        let attachment = null;
        if (attachmentId) {
            const file = files.get(attachmentId);
            // Chỉ người tải tệp lên mới được đính kèm tệp đó vào tin nhắn.
            if (!file || file.owner !== client.userId) {
                return emit(client.socket, "/error", { code: "BAD_ATTACHMENT", message: "Tệp đính kèm không hợp lệ" });
            }
            attachment = {
                id: attachmentId,
                filename: file.filename,
                contentType: file.contentType,
                size: file.buffer.length,
            };
        }

        const message = {
            id: newId(),
            conversationId,
            senderId: client.userId,
            senderName: client.name,
            text,
            time: hhmm(),
            attachment,
        };
        group.messages.push(message);
        notifyGroup(group, { type: "message", message });
        return;
    }

    emit(client.socket, "/error", { code: "UNKNOWN_ROUTE", message: "Không hỗ trợ thao tác này" });
}

const wss = new WebSocketServer({ port: WS_PORT, path: "/ws" });

wss.on("connection", (socket, req) => {
    const ticket = new URL(req.url, `http://localhost:${WS_PORT}`).searchParams.get("ticket") ?? "";
    const username = consumeTicket(ticket);
    if (!username) {
        socket.close(4401, "unauthorized");
        return;
    }
    if (Date.now() < (blockedUntil.get(username) ?? 0)) {
        socket.close(1013, "try again later");
        return;
    }
    const client = { socket, userId: username, name: nameOf(username) };
    clients.add(client);
    console.log(`[ws] đã kết nối: ${username}`);
    emit(socket, "/session", { userId: client.userId, name: client.name });
    socket.on("message", (raw) => handleFrame(client, raw.toString()));
    socket.on("close", () => clients.delete(client));
});

httpServer.listen(HTTP_PORT, () => {
    console.log(`Mock HTTP: http://localhost:${HTTP_PORT}`);
});
console.log(`Mock WebSocket: ws://localhost:${WS_PORT}/ws`);
console.log("Tài khoản test: sv1, sv2, sv3, gv1 | mật khẩu: Test1234");
console.log("Nhóm có sẵn: g1 (sv1, sv2), g2 (gv1, sv3)");