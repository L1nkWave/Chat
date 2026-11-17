type NoticeProps = {
    text: string;
    tone: "info" | "error";
    onClose?: () => void;
};

/** Thông báo trong form hoặc khối nội dung. Lỗi dùng role="alert" để đọc ngay. */
export function Notice({ text, tone, onClose }: NoticeProps) {
    const isError = tone === "error";
    return (
        <div
            role={isError ? "alert" : "status"}
            className={`flex items-start gap-3 rounded-md border px-3 py-2 text-sm leading-relaxed ${
                isError ? "border-danger text-danger" : "border-line bg-tint text-ink"
            }`}
        >
            <p className="min-w-0 flex-1">{text}</p>
            {onClose ? (
                <button
                    type="button"
                    onClick={onClose}
                    aria-label="Đóng thông báo"
                    className="shrink-0 rounded px-1 text-lg leading-none text-muted transition-colors hover:text-ink"
                >
                    ×
                </button>
            ) : null}
        </div>
    );
}