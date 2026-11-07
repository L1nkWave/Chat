import WebSocket from "ws";

const token = process.env.TOKEN;
if (!token) throw new Error("Thiếu biến môi trường TOKEN");

const res = await fetch("http://localhost:9090/api/v1/ws-tickets", {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
});
const body = await res.json();
console.log("issue ticket:", res.status, body);
if (!res.ok) process.exit(1);

function connect(label) {
    const ws = new WebSocket(`ws://localhost:9090/ws?ticket=${encodeURIComponent(body.ticket)}`);
    ws.on("open", () => console.log(label, "OPEN"));
    ws.on("unexpected-response", (_req, r) => console.log(label, "REJECTED HTTP", r.statusCode));
    ws.on("error", (e) => console.log(label, "ERROR", e.message));
    ws.on("close", (code) => console.log(label, "CLOSE", code));
    return ws;
}

const first = connect("lần 1");
setTimeout(() => {
    const second = connect("lần 2 (cùng vé)");
    setTimeout(() => { first.close(); second.close(); }, 1000);
}, 1000);