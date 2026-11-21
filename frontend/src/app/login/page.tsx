"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";

import { AuthLayout } from "@/components/auth/AuthLayout";
import { Button } from "@/components/ui/Button";
import { Notice } from "@/components/ui/Notice";
import { TextField } from "@/components/ui/TextField";
import { LINK } from "@/components/ui/styles";
import { ApiError } from "@/lib/api/client";
import { login, verifyTwoFactor } from "@/lib/auth/auth-api";
import { isAdminUser } from "@/lib/auth/token";
import { hasErrors, validateLogin } from "@/lib/auth/validation";
import type { FieldErrors, LoginFields } from "@/lib/auth/validation";

export default function LoginPage() {
    const router = useRouter();
    const [fields, setFields] = useState<LoginFields>({ username: "", password: "" });
    const [errors, setErrors] = useState<FieldErrors<keyof LoginFields>>({});
    const [serverError, setServerError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);

    // Sau khi bật 2FA, trang thiết lập chuyển về đây với ?twofa=on
    const [info, setInfo] = useState<string | null>(null);
    useEffect(() => {
        if (new URLSearchParams(window.location.search).get("twofa") === "on") {
            setInfo("Đã bật xác thực hai lớp. Vui lòng đăng nhập lại bằng mật khẩu và mã xác thực.");
            window.history.replaceState(null, "", "/login");
        }
    }, []);

    // Có giá trị khi ADMIN/ROOT đã bật 2FA: chuyển sang bước nhập mã
    const [challenge, setChallenge] = useState<string | null>(null);
    const [code, setCode] = useState("");
    const [codeError, setCodeError] = useState<string | undefined>(undefined);

    function handleChange(event: ChangeEvent<HTMLInputElement>) {
        const { name, value } = event.target;
        setFields((prev) => ({ ...prev, [name]: value }));
    }

    function finishSignIn() {
        router.replace(isAdminUser() ? "/admin" : "/chat");
    }

    async function handleCredentials(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setServerError(null);

        const nextErrors = validateLogin(fields);
        setErrors(nextErrors);
        if (hasErrors(nextErrors)) {
            return;
        }

        setSubmitting(true);
        try {
            const result = await login(fields.username.trim(), fields.password);
            if (result.kind === "twoFactor") {
                setChallenge(result.challenge);
                setSubmitting(false);
                return;
            }
            finishSignIn();
        } catch (error) {
            setServerError(describeLoginError(error));
            setSubmitting(false);
        }
    }

    async function handleCode(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setServerError(null);

        const value = code.trim();
        if (!value) {
            setCodeError("Vui lòng nhập mã xác thực.");
            return;
        }
        setCodeError(undefined);
        if (!challenge) return;

        setSubmitting(true);
        try {
            await verifyTwoFactor(challenge, value);
            finishSignIn();
        } catch (error) {
            setServerError(describeTwoFactorError(error));
            setSubmitting(false);
        }
    }

    function backToCredentials() {
        setChallenge(null);
        setCode("");
        setCodeError(undefined);
        setServerError(null);
    }

    if (challenge) {
        return (
            <AuthLayout
                title="Xác thực hai lớp"
                subtitle="Nhập mã 6 số từ ứng dụng xác thực, hoặc một mã khôi phục chưa dùng."
                footer={
                    <button
                        type="button"
                        onClick={backToCredentials}
                        className={`${LINK} cursor-pointer`}
                    >
                        Quay lại đăng nhập
                    </button>
                }
            >
                <form noValidate onSubmit={handleCode} className="flex flex-col gap-5" aria-busy={submitting}>
                    <TextField
                        label="Mã xác thực"
                        name="code"
                        autoComplete="one-time-code"
                        autoCapitalize="none"
                        spellCheck={false}
                        value={code}
                        onChange={(event) => setCode(event.target.value)}
                        error={codeError}
                        hint="Mã 6 số đổi mỗi 30 giây. Mã khôi phục chỉ dùng được một lần."
                    />

                    {serverError ? <Notice text={serverError} tone="error" /> : null}

                    <Button type="submit" disabled={submitting}>
                        {submitting ? "Đang xác thực..." : "Xác nhận"}
                    </Button>
                </form>
            </AuthLayout>
        );
    }

    return (
        <AuthLayout
            title="Đăng nhập"
            subtitle="Dùng tên đăng nhập và mật khẩu của bạn."
            footer={<p>Tài khoản do nhà trường cấp. Nếu chưa có tài khoản, vui lòng liên hệ phòng ban phụ trách.</p>}
        >
            <form noValidate onSubmit={handleCredentials} className="flex flex-col gap-5" aria-busy={submitting}>
                {info ? <Notice text={info} tone="info" onClose={() => setInfo(null)} /> : null}

                <TextField
                    label="Tên đăng nhập"
                    name="username"
                    autoComplete="username"
                    autoCapitalize="none"
                    spellCheck={false}
                    value={fields.username}
                    onChange={handleChange}
                    error={errors.username}
                />

                <TextField
                    label="Mật khẩu"
                    name="password"
                    type="password"
                    autoComplete="current-password"
                    value={fields.password}
                    onChange={handleChange}
                    error={errors.password}
                />

                {serverError ? <Notice text={serverError} tone="error" /> : null}

                <Button type="submit" disabled={submitting}>
                    {submitting ? "Đang đăng nhập..." : "Đăng nhập"}
                </Button>
            </form>
        </AuthLayout>
    );
}

function describeLoginError(error: unknown): string {
    if (error instanceof ApiError) {
        if (error.status === 403) {
            return "Tài khoản đã bị khóa hoặc đã bị xóa. Vui lòng liên hệ phòng ban phụ trách.";
        }
        if (error.status === 400 || error.status === 401) {
            return "Tên đăng nhập hoặc mật khẩu không đúng.";
        }
        if (error.status === 429) {
            return "Bạn đã thử đăng nhập quá nhiều lần. Vui lòng đợi một lúc rồi thử lại.";
        }
        if (error.status >= 500) {
            return "Máy chủ đang gặp sự cố. Vui lòng thử lại sau.";
        }
    }
    return "Không kết nối được máy chủ. Vui lòng thử lại sau.";
}

function describeTwoFactorError(error: unknown): string {
    if (error instanceof ApiError) {
        if (error.status === 429) {
            return "Nhập sai quá nhiều lần. Vui lòng thử lại sau 5 phút.";
        }
        if (error.status === 400 || error.status === 401) {
            return "Mã không đúng hoặc phiên xác thực đã hết hạn (5 phút). Hãy kiểm tra lại hoặc đăng nhập lại.";
        }
        if (error.status >= 500) {
            return "Máy chủ đang gặp sự cố. Vui lòng thử lại sau.";
        }
    }
    return "Không kết nối được máy chủ. Vui lòng thử lại sau.";
}