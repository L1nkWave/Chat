"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import type { FormEvent } from "react";
import QRCode from "qrcode";

import { AuthLayout } from "@/components/auth/AuthLayout";
import { Button } from "@/components/ui/Button";
import { Notice } from "@/components/ui/Notice";
import { TextField } from "@/components/ui/TextField";
import { ApiError } from "@/lib/api/client";
import { confirmTwoFactor, disableTwoFactor, logout, setupTwoFactor } from "@/lib/auth/auth-api";
import { useAuthGate } from "@/lib/auth/useAuthGate";

type SetupState = {
    secret: string;
    qrDataUrl: string;
};

type Feedback = { text: string; tone: "info" | "error" };

export default function TwoFactorPage() {
    const router = useRouter();
    const { status } = useAuthGate(true);

    const [setup, setSetup] = useState<SetupState | null>(null);
    const [recoveryCodes, setRecoveryCodes] = useState<string[] | null>(null);
    const [code, setCode] = useState("");
    const [busy, setBusy] = useState(false);
    const [notice, setNotice] = useState<Feedback | null>(null);

    if (status !== "ready") {
        return (
            <AuthLayout
                title="Xác thực hai lớp"
                subtitle={status === "forbidden" ? "Trang này chỉ dành cho ADMIN và ROOT." : "Đang kiểm tra phiên đăng nhập..."}
                footer={<Link href="/chat" className="text-primary underline-offset-4 hover:underline">Về trang chat</Link>}
            >
                <p className="text-sm text-muted">{status === "forbidden" ? "Bạn không có quyền truy cập." : ""}</p>
            </AuthLayout>
        );
    }

    async function startSetup() {
        setBusy(true);
        setNotice(null);
        try {
            const res = await setupTwoFactor();
            // Mã QR được tạo ngay trên trình duyệt bằng thư viện đã cài sẵn, không gọi dịch vụ ngoài
            const qrDataUrl = await QRCode.toDataURL(res.otpauthUri, { width: 224, margin: 1 });
            setSetup({ secret: res.secret, qrDataUrl });
            setCode("");
        } catch (err) {
            setNotice({ text: describe(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    async function confirm(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const value = code.trim();
        if (!/^\d{6}$/.test(value)) {
            setNotice({ text: "Mã xác thực gồm 6 chữ số.", tone: "error" });
            return;
        }
        setBusy(true);
        setNotice(null);
        try {
            const codes = await confirmTwoFactor(value);
            setRecoveryCodes(codes);
            setSetup(null);
            setCode("");
            // Bật 2FA xong thì đăng xuất ngay. Mã khôi phục đã nằm trong trang để hiện một lần.
            void logout().catch(() => undefined);
        } catch (err) {
            setNotice({ text: describe(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    async function disable(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const value = code.trim();
        if (!value) {
            setNotice({ text: "Nhập mã xác thực hoặc mã khôi phục để tắt 2FA.", tone: "error" });
            return;
        }
        if (!window.confirm("Tắt xác thực hai lớp? Tài khoản sẽ chỉ còn mật khẩu.")) return;
        setBusy(true);
        setNotice(null);
        try {
            await disableTwoFactor(value);
            setCode("");
            setNotice({ text: "Đã tắt xác thực hai lớp.", tone: "info" });
        } catch (err) {
            setNotice({ text: describe(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    async function copyCodes(codes: string[]) {
        try {
            await navigator.clipboard.writeText(codes.join("\n"));
            setNotice({ text: "Đã sao chép mã khôi phục.", tone: "info" });
        } catch {
            setNotice({ text: "Không sao chép được. Hãy tải về hoặc ghi lại mã.", tone: "error" });
        }
    }

    function downloadCodes(codes: string[]) {
        const content = `SGU Chat - Mã khôi phục xác thực hai lớp\n\n${codes.join("\n")}\n`;
        const url = URL.createObjectURL(new Blob([content], { type: "text/plain;charset=utf-8" }));
        const link = document.createElement("a");
        link.href = url;
        link.download = "sgu-chat-ma-khoi-phuc.txt";
        link.click();
        URL.revokeObjectURL(url);
    }

    if (recoveryCodes) {
        return (
            <AuthLayout
                title="Mã khôi phục"
                subtitle="Lưu lại 8 mã này ở nơi an toàn. Mỗi mã dùng được một lần và chỉ hiện một lần duy nhất. Bạn đã được đăng xuất, hãy đăng nhập lại sau khi lưu mã."
                footer={<span className="text-muted">Đã đăng xuất để bảo mật.</span>}
            >
                <ol className="grid grid-cols-2 gap-2 rounded-md border border-line bg-paper p-4 font-mono text-sm">
                    {recoveryCodes.map((item) => (
                        <li key={item}>{item}</li>
                    ))}
                </ol>

                <div className="flex flex-wrap gap-2">
                    <button
                        type="button"
                        onClick={() => copyCodes(recoveryCodes)}
                        className="h-10 rounded-md border border-line px-4 text-sm text-ink transition-colors hover:bg-tint"
                    >
                        Sao chép
                    </button>
                    <button
                        type="button"
                        onClick={() => downloadCodes(recoveryCodes)}
                        className="h-10 rounded-md border border-line px-4 text-sm text-ink transition-colors hover:bg-tint"
                    >
                        Tải về (.txt)
                    </button>
                </div>

                {notice ? <Notice text={notice.text} tone={notice.tone} /> : null}

                <Button onClick={() => router.replace("/login?twofa=on")}>Tôi đã lưu mã, đăng nhập lại</Button>
            </AuthLayout>
        );
    }

    return (
        <AuthLayout
            title="Xác thực hai lớp"
            subtitle="Dùng ứng dụng xác thực (Google Authenticator, Microsoft Authenticator...) để lấy mã 6 số mỗi lần đăng nhập."
            footer={<Link href="/admin" className="text-primary underline-offset-4 hover:underline">Quay lại trang quản trị</Link>}
        >
            {setup ? (
                <div className="flex flex-col gap-5">
                    <p className="text-sm leading-relaxed">
                        Quét mã QR bằng ứng dụng xác thực, hoặc nhập thủ công khóa bí mật bên dưới rồi nhập mã 6 số để xác nhận.
                    </p>
                    <div className="flex flex-col items-center gap-3">
                        {/* Ảnh QR được tạo từ dữ liệu trên trình duyệt (data URL), nên không dùng next/image */}
                        {/* eslint-disable-next-line @next/next/no-img-element */}
                        <img
                            src={setup.qrDataUrl}
                            alt="Mã QR để thêm tài khoản SGU Chat vào ứng dụng xác thực"
                            width={224}
                            height={224}
                            className="rounded-md border border-line bg-white p-2"
                        />
                    </div>
                    <div className="flex flex-col gap-1">
                        <span className="text-xs font-semibold uppercase tracking-wide text-muted">Khóa bí mật</span>
                        <code className="break-all rounded-md border border-line bg-paper px-3 py-2 font-mono text-sm">
                            {setup.secret}
                        </code>
                    </div>

                    <form noValidate onSubmit={confirm} className="flex flex-col gap-4" aria-busy={busy}>
                        <TextField
                            label="Mã 6 số từ ứng dụng"
                            name="totp"
                            inputMode="numeric"
                            autoComplete="one-time-code"
                            maxLength={6}
                            value={code}
                            onChange={(event) => setCode(event.target.value)}
                        />
                        {notice ? <Notice text={notice.text} tone={notice.tone} /> : null}
                        <Button type="submit" disabled={busy}>
                            {busy ? "Đang xác nhận..." : "Xác nhận và bật 2FA"}
                        </Button>
                    </form>
                </div>
            ) : (
                <div className="flex flex-col gap-6">
                    <div className="flex flex-col gap-3">
                        <h2 className="text-sm font-semibold">Bật xác thực hai lớp</h2>
                        <p className="text-sm leading-relaxed text-muted">
                            Sau khi bật, mỗi lần ADMIN hoặc ROOT đăng nhập sẽ cần thêm mã 6 số.
                        </p>
                        <Button onClick={startSetup} disabled={busy} className="self-start">
                            {busy ? "Đang tạo..." : "Thiết lập 2FA"}
                        </Button>
                    </div>

                    <form noValidate onSubmit={disable} className="flex flex-col gap-3 border-t border-line pt-5">
                        <h2 className="text-sm font-semibold">Tắt xác thực hai lớp</h2>
                        <TextField
                            label="Mã xác thực hoặc mã khôi phục"
                            name="disableCode"
                            autoComplete="one-time-code"
                            spellCheck={false}
                            value={code}
                            onChange={(event) => setCode(event.target.value)}
                        />
                        {notice ? <Notice text={notice.text} tone={notice.tone} /> : null}
                        <Button type="submit" variant="outline" disabled={busy}>
                            Tắt 2FA
                        </Button>
                    </form>
                </div>
            )}
        </AuthLayout>
    );
}

function describe(err: unknown): string {
    if (err instanceof ApiError) return err.message;
    return "Đã có lỗi xảy ra. Vui lòng thử lại.";
}