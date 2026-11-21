// Tách liên kết trong tin nhắn. Chỉ http và https mới thành liên kết bấm được;
// javascript:, data:, vbscript: và mọi lược đồ khác chỉ hiện như văn bản.

export type TextPart = { text: string; url: string | null };

// Khớp http(s)://..., www.... và các lược đồ nguy hiểm (để chặn, không để thành liên kết)
const CANDIDATE = /((?:https?:\/\/|www\.)[^\s<>"']+|(?:javascript|data|vbscript):[^\s<>"']*)/gi;

// Dấu câu ở cuối liên kết (ví dụ "xem tại https://a.edu.vn.") không thuộc URL
const TRAILING_PUNCT = /^(.*?)([.,;:!?)\]}]*)$/;

/** Trả về URL tuyệt đối an toàn (http/https) hoặc null nếu không hợp lệ. */
export function toSafeUrl(raw: string): string | null {
    const candidate = /^https?:\/\//i.test(raw) ? raw : /^www\./i.test(raw) ? `https://${raw}` : null;
    if (!candidate) return null;
    try {
        const parsed = new URL(candidate);
        if (parsed.protocol !== "http:" && parsed.protocol !== "https:") return null;
        if (!parsed.hostname || parsed.hostname.endsWith(".")) return null;
        return parsed.href;
    } catch {
        return null;
    }
}

export function splitLinks(text: string): TextPart[] {
    const parts: TextPart[] = [];
    let last = 0;
    for (const match of text.matchAll(CANDIDATE)) {
        const start = match.index ?? 0;
        const whole = match[0];
        const tail = TRAILING_PUNCT.exec(whole);
        const body = tail ? tail[1] : whole;
        const trailing = tail ? tail[2] : "";

        if (start > last) parts.push({ text: text.slice(last, start), url: null });
        if (body) parts.push({ text: body, url: toSafeUrl(body) });
        if (trailing) parts.push({ text: trailing, url: null });
        last = start + whole.length;
    }
    if (last < text.length) parts.push({ text: text.slice(last), url: null });
    return parts;
}
