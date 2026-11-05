"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import type { ChangeEvent, FormEvent } from "react";

import { AuthLayout } from "@/components/auth/AuthLayout";
import { Button } from "@/components/ui/Button";
import { TextField } from "@/components/ui/TextField";
import { ApiError } from "@/lib/api/client";
import { login, register } from "@/lib/auth/auth-api";
import { hasErrors, validateSignup } from "@/lib/auth/validation";
import type { FieldErrors, SignupFields } from "@/lib/auth/validation";

const EMPTY_FIELDS: SignupFields = {
    name: "",
    username: "",
    password: "",
    confirmPassword: "",
};

export default function SignupPage() {
    const router = useRouter();
    const [fields, setFields] = useState<SignupFields>(EMPTY_FIELDS);
    const [errors, setErrors] = useState<FieldErrors<keyof SignupFields>>({});
    const [serverError, setServerError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);

    function handleChange(event: ChangeEvent<HTMLInputElement>) {
        const { name, value } = event.target;
        setFields((prev) => ({ ...prev, [name]: value }));
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setServerError(null);

        const nextErrors = validateSignup(fields);
        setErrors(nextErrors);
        if (hasErrors(nextErrors)) {
            return;
        }

        setSubmitting(true);
        const username = fields.username.trim();
        try {
            await register({ username, password: fields.password, name: fields.name.trim() });
            // Sign in right after registering so the user lands in the chat without a second form
            await login(username, fields.password);
            router.replace("/chat");
        } catch (error) {
            setServerError(describeSignupError(error));
            setSubmitting(false);
        }
    }

    return (
        <AuthLayout
            title="Tạo tài khoản"
            subtitle="Tên đăng nhập sẽ dùng để tìm và nhắn tin với bạn."
            footer={
                <p>
                    Đã có tài khoản?{" "}
                    <Link href="/login" className="font-medium text-primary underline-offset-4 hover:underline">
                        Đăng nhập
                    </Link>
                </p>
            }
        >
            <form noValidate onSubmit={handleSubmit} className="flex flex-col gap-5" aria-busy={submitting}>
                <TextField
                    label="Họ và tên"
                    name="name"
                    autoComplete="name"
                    value={fields.name}
                    onChange={handleChange}
                    error={errors.name}
                />

                <TextField
                    label="Tên đăng nhập"
                    name="username"
                    autoComplete="username"
                    autoCapitalize="none"
                    spellCheck={false}
                    value={fields.username}
                    onChange={handleChange}
                    error={errors.username}
                    hint="3 đến 32 ký tự: chữ không dấu, số, dấu gạch dưới hoặc dấu chấm."
                />

                <TextField
                    label="Mật khẩu"
                    name="password"
                    type="password"
                    autoComplete="new-password"
                    value={fields.password}
                    onChange={handleChange}
                    error={errors.password}
                    hint="8 đến 64 ký tự, gồm ít nhất một chữ cái và một chữ số."
                />

                <TextField
                    label="Nhập lại mật khẩu"
                    name="confirmPassword"
                    type="password"
                    autoComplete="new-password"
                    value={fields.confirmPassword}
                    onChange={handleChange}
                    error={errors.confirmPassword}
                />

                {serverError ? (
                    <p role="alert" className="rounded-md border border-danger px-3 py-2 text-sm text-danger">
                        {serverError}
                    </p>
                ) : null}

                <Button type="submit" disabled={submitting}>
                    {submitting ? "Đang tạo tài khoản..." : "Tạo tài khoản"}
                </Button>
            </form>
        </AuthLayout>
    );
}

function describeSignupError(error: unknown): string {
    if (error instanceof ApiError) {
        if (error.status === 409) {
            return "Tên đăng nhập này đã được sử dụng. Hãy chọn tên khác.";
        }
        if (error.status === 400) {
            return "Thông tin chưa hợp lệ. Vui lòng kiểm tra lại các trường đã nhập.";
        }
        if (error.status >= 500) {
            return "Máy chủ đang gặp sự cố. Vui lòng thử lại sau.";
        }
    }
    return "Không kết nối được máy chủ. Vui lòng thử lại sau.";
}