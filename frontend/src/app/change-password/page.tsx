"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import type { ChangeEvent, FormEvent } from "react";

import { AuthLayout } from "@/components/auth/AuthLayout";
import { Button } from "@/components/ui/Button";
import { Notice } from "@/components/ui/Notice";
import { TextField } from "@/components/ui/TextField";
import { ApiError } from "@/lib/api/client";
import { changePassword } from "@/lib/auth/auth-api";
import { isPasswordChangeRequired } from "@/lib/auth/session";
import { useAuthGate } from "@/lib/auth/useAuthGate";

type Fields = {
    oldPassword: string;
    newPassword: string;
    confirmPassword: string;
};

type Errors = Partial<Record<keyof Fields, string>>;

const EMPTY_FIELDS: Fields = { oldPassword: "", newPassword: "", confirmPassword: "" };

// Kiểm tra nhanh phía trình duyệt. Máy chủ kiểm tra lại và là nơi quyết định cuối cùng.
function validate(fields: Fields): Errors {
    const errors: Errors = {};
    if (!fields.oldPassword) {
        errors.oldPassword = "Vui lòng nhập mật khẩu hiện tại.";
    }
    const next = fields.newPassword;
    if (next.length < 8 || next.length > 64) {
        errors.newPassword = "Mật khẩu mới cần từ 8 đến 64 ký tự.";
    } else if (!/[A-Za-z]/.test(next) || !/\d/.test(next)) {
        errors.newPassword = "Mật khẩu mới cần có ít nhất một chữ cái và một chữ số.";
    } else if (next === fields.oldPassword) {
        errors.newPassword = "Mật khẩu mới phải khác mật khẩu hiện tại.";
    }
    if (fields.confirmPassword !== next) {
        errors.confirmPassword = "Mật khẩu nhập lại chưa khớp.";
    }
    return errors;
}

function describeError(error: unknown): string {
    if (error instanceof ApiError) {
        return error.message;
    }
    return "Không kết nối được máy chủ. Vui lòng thử lại sau.";
}

export default function ChangePasswordPage() {
    const router = useRouter();
    const { status } = useAuthGate(false, false, true);

    const [fields, setFields] = useState<Fields>(EMPTY_FIELDS);
    const [errors, setErrors] = useState<Errors>({});
    const [serverError, setServerError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);

    if (status !== "ready") {
        return (
            <AuthLayout
                title="Đổi mật khẩu"
                subtitle="Đang kiểm tra phiên đăng nhập..."
                footer={<Link href="/login" className="text-primary underline-offset-4 hover:underline">Quay lại đăng nhập</Link>}
            >
                <p className="text-sm text-muted"></p>
            </AuthLayout>
        );
    }

    const required = isPasswordChangeRequired();

    function handleChange(event: ChangeEvent<HTMLInputElement>) {
        const { name, value } = event.target;
        setFields((prev) => ({ ...prev, [name]: value }));
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setServerError(null);

        const nextErrors = validate(fields);
        setErrors(nextErrors);
        if (Object.keys(nextErrors).length > 0) {
            return;
        }

        setSubmitting(true);
        try {
            await changePassword(fields.oldPassword, fields.newPassword);
            // Máy chủ đã thu hồi phiên: quay về đăng nhập bằng mật khẩu mới
            router.replace("/login");
        } catch (error) {
            setServerError(describeError(error));
            setSubmitting(false);
        }
    }

    return (
        <AuthLayout
            title="Đổi mật khẩu"
            subtitle={
                required
                    ? "Tài khoản của bạn cần đổi mật khẩu trước khi tiếp tục. Sau khi đổi, bạn sẽ đăng nhập lại."
                    : "Sau khi đổi, mọi phiên đăng nhập sẽ bị đăng xuất và bạn cần đăng nhập lại."
            }
            footer={
                <Link href="/chat" className="text-primary underline-offset-4 hover:underline">
                    Về trang chat
                </Link>
            }
        >
            <form noValidate onSubmit={handleSubmit} className="flex flex-col gap-5" aria-busy={submitting}>
                <TextField
                    label="Mật khẩu hiện tại"
                    name="oldPassword"
                    type="password"
                    autoComplete="current-password"
                    value={fields.oldPassword}
                    onChange={handleChange}
                    error={errors.oldPassword}
                />

                <TextField
                    label="Mật khẩu mới"
                    name="newPassword"
                    type="password"
                    autoComplete="new-password"
                    value={fields.newPassword}
                    onChange={handleChange}
                    error={errors.newPassword}
                    hint="Từ 8 đến 64 ký tự, gồm cả chữ và số."
                />

                <TextField
                    label="Nhập lại mật khẩu mới"
                    name="confirmPassword"
                    type="password"
                    autoComplete="new-password"
                    value={fields.confirmPassword}
                    onChange={handleChange}
                    error={errors.confirmPassword}
                />

                {serverError ? <Notice text={serverError} tone="error" /> : null}

                <Button type="submit" disabled={submitting}>
                    {submitting ? "Đang lưu..." : "Đổi mật khẩu"}
                </Button>
            </form>
        </AuthLayout>
    );
}