const EVENT = "sgu:academic-changed";

/** Báo cho mọi bảng và danh sách học vụ đang mở biết dữ liệu vừa thay đổi, để tải lại ngay. */
export function notifyAcademicChanged(): void {
    window.dispatchEvent(new Event(EVENT));
}

/**
 * Gọi `onChange` mỗi khi có thay đổi học vụ. Trả về hàm hủy để gỡ lắng nghe.
 * Dùng trong useEffect: `useEffect(() => onAcademicChanged(reload), [reload])`.
 */
export function onAcademicChanged(onChange: () => void): () => void {
    window.addEventListener(EVENT, onChange);
    return () => window.removeEventListener(EVENT, onChange);
}