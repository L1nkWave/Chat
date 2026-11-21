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
            className={`flex items-start gap-3 rounded-md border px-3.5 py-2.5 text-sm leading-relaxed text-ink ${
                isError ? "border-danger/40 bg-danger/5" : "border-line bg-tint"
            }`}
        >
            <p className="min-w-0 flex-1">{text}</p>
            {onClose ? (
                <button
                    type="button"
                    onClick={onClose}
                    aria-label="Đóng thông báo"
                    className="-my-1 grid size-7 shrink-0 place-items-center rounded text-lg leading-none text-muted transition-colors hover:bg-surface hover:text-ink"
                >
                    ×
                </button>
            ) : null}
        </div>
    );
}
