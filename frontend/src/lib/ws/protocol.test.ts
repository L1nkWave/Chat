import { test } from "node:test";
import assert from "node:assert/strict";
import { decodeFrame, encodeFrame } from "./protocol";

test("encode rồi decode trả về đúng route và payload", () => {
    const raw = encodeFrame("/chat", { type: "send", text: "xin chào" });
    assert.deepEqual(decodeFrame(raw), {
        path: "/chat",
        payload: { type: "send", text: "xin chào" },
    });
});

test("chấp nhận khung có xuống dòng kiểu CRLF", () => {
    const raw = "path=/session\r\n\r\n{\"userId\":\"sv1\",\"name\":\"Sinh viên 1\"}";
    assert.deepEqual(decodeFrame(raw), {
        path: "/session",
        payload: { userId: "sv1", name: "Sinh viên 1" },
    });
});

test("từ chối khung thiếu dòng trống", () => {
    assert.equal(decodeFrame("path=/chat"), null);
});

test("từ chối khung thiếu tiền tố path=", () => {
    assert.equal(decodeFrame("/chat\n\n{}"), null);
});

test("từ chối JSON hỏng", () => {
    assert.equal(decodeFrame("path=/chat\n\n{oops"), null);
});