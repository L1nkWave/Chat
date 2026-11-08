/**
 * Định dạng thời gian tin nhắn: dd/mm/yyyy · HH:mm (theo giờ của trình duyệt).
 * Nếu không đọc được thì giữ nguyên chuỗi gốc.
 */
export function formatChatTime(raw: string): string {
    if (!raw) return "";
    // Server có thể trả 9 chữ số phần giây (nano), JS chỉ đọc được tới mili giây.
    const normalized = raw.replace(/(\.\d{3})\d+/, "$1");
    const date = new Date(normalized);
    if (Number.isNaN(date.getTime())) return raw;

    const pad = (n: number) => String(n).padStart(2, "0");
    const day = pad(date.getDate());
    const month = pad(date.getMonth() + 1);
    const year = date.getFullYear();
    const hour = pad(date.getHours());
    const minute = pad(date.getMinutes());
    return `${day}/${month}/${year} · ${hour}:${minute}`;
}