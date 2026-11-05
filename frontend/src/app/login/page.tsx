"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import type { ChangeEvent, FormEvent } from "react";

import { AuthLayout } from "@/components/auth/AuthLayout";
import { Button } from "@/components/ui/Button";
import { TextField } from "@/components/ui/TextField";
import { ApiError } from "@/lib/api/client";
import { login } from "@/lib/auth/auth-api";
import { hasErrors, validateLogin } from "@/lib/auth/validation";
import type { FieldErrors, LoginFields } from "@/lib/auth/validation";

export default function LoginPage() {
    const router = useRouter();
    const [fields, setFields] = useState<LoginFields>({ username: "", password: "" });
    const [errors, setErrors] = useState<FieldErrors<keyof LoginFields>>({});
    const [serverError, setServerError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);

    function handleChange(event: ChangeEvent<HTMLInputElement>) {
        const { name, value } = event.target;
        setFields((prev) => ({ ...prev, [name]: value }));
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setServerError(null);

        const nextErrors = validateLogin(fields);
        setErrors(nextErrors);
        if (hasErrors(nextErrors)) {
            return;
        }

        setSubmitting(true);
        try {
            await login(fields.username.trim(), fields.password);
            router.replace("/chat");
        } catch (error) {
            setServerError(describeLoginError(error));
            setSubmitting(false);
        }
    }

    return (
        <AuthLayout
            title="Đăng nhập"
            subtitle="Dùng tên đăng nhập và mật khẩu của bạn."
            footer={
                <p>
                    Chưa có tài khoản?{" "}
                    <Link href="/signup" className="font-medium text-primary underline-offset-4 hover:underline">
                        Tạo tài khoản
                    </Link>
                </p>
            }
        >
            <form noValidate onSubmit={handleSubmit} className="flex flex-col gap-5" aria-busy={submitting}>
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

                {serverError ? (
                    <p role="alert" className="rounded-md border border-danger px-3 py-2 text-sm text-danger">
                        {serverError}
                    </p>
                ) : null}

                <Button type="submit" disabled={submitting}>
                    {submitting ? "Đang đăng nhập..." : "Đăng nhập"}
                </Button>
            </form>
        </AuthLayout>
    );
}

function describeLoginError(error: unknown): string {
    if (error instanceof ApiError) {
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