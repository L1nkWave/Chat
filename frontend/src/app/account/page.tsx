"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useId, useState } from "react";
import type { FormEvent } from "react";

import { GENDER_LABEL, ROLE_LABEL, STATUS_LABEL } from "@/components/admin/labels";
import { ThemeToggle } from "@/components/theme/ThemeToggle";
import { Button, buttonClasses } from "@/components/ui/Button";
import { Notice } from "@/components/ui/Notice";
import { PageHeader } from "@/components/ui/PageHeader";
import { TextField } from "@/components/ui/TextField";
import { CARD, HELP, LINK, SECTION_TITLE } from "@/components/ui/styles";
import { adminErrorMessage } from "@/lib/admin/api";
import { getMyProfile, logoutAllDevices, updateMyContact } from "@/lib/account/account-api";
import type { MyProfileView } from "@/lib/account/account-api";
import { useAuthGate } from "@/lib/auth/useAuthGate";
import { hasMfa, isAdminUser } from "@/lib/auth/token";

type Feedback = { text: string; tone: "info" | "error" };

const DATE_FORMAT = new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });

function show(value: string | null | undefined): string {
    return value && value.trim() ? value : "—";
}

function formatDate(iso: string | null): string {
    if (!iso) return "—";
    const [year, month, day] = iso.split("-").map(Number);
    return DATE_FORMAT.format(new Date(year, month - 1, day));
}

export default function AccountPage() {
    const router = useRouter();
    const { status } = useAuthGate(false);
    const [me, setMe] = useState<MyProfileView | null>(null);
    const [notice, setNotice] = useState<Feedback | null>(null);
    const [busy, setBusy] = useState(false);

    const phoneId = useId();
    const addressId = useId();
    const [phone, setPhone] = useState("");
    const [address, setAddress] = useState("");

    useEffect(() => {
        if (status !== "ready") return;
        getMyProfile()
            .then((view) => {
                setMe(view);
                setPhone(view.profile.phone ?? "");
                setAddress(view.profile.address ?? "");
            })
            .catch((err) => setNotice({ text: adminErrorMessage(err), tone: "error" }));
    }, [status]);

    if (status !== "ready") {
        return (
            <div className="flex min-h-dvh items-center justify-center bg-paper px-4 text-ink">
                <p className="text-sm text-muted">
                    {status === "forbidden" ? "Bạn không có quyền truy cập." : "Đang kiểm tra phiên đăng nhập..."}
                </p>
            </div>
        );
    }

    const isAdmin = isAdminUser();
    const roleText = me ? me.roles.map((r) => ROLE_LABEL[r] ?? r).join(", ") : "";

    async function saveContact(event: FormEvent) {
        event.preventDefault();
        setBusy(true);
        setNotice(null);
        try {
            const updated = await updateMyContact(phone.trim(), address.trim());
            setMe(updated);
            setPhone(updated.profile.phone ?? "");
            setAddress(updated.profile.address ?? "");
            setNotice({ text: "Đã lưu số điện thoại và địa chỉ.", tone: "info" });
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
        } finally {
            setBusy(false);
        }
    }

    async function signOutEverywhere() {
        if (!window.confirm("Đăng xuất tất cả thiết bị? Bạn sẽ phải đăng nhập lại ở mọi nơi.")) return;
        setBusy(true);
        try {
            await logoutAllDevices();
            router.replace("/login");
        } catch (err) {
            setNotice({ text: adminErrorMessage(err), tone: "error" });
            setBusy(false);
        }
    }

    return (
        <div className="flex min-h-dvh flex-col bg-paper text-ink">
            <header className="mx-auto flex w-full max-w-2xl items-center justify-between gap-3 px-4 pt-4 sm:px-8">
                <nav aria-label="Liên kết nhanh" className="text-sm">
                    <Link href="/chat" className={LINK}>
                        Về trang chat
                    </Link>
                </nav>
                <ThemeToggle />
            </header>

            <main className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-8 px-4 pb-16 pt-6 sm:px-8">
                <PageHeader title="Tài khoản" description={me ? `${me.name} · @${me.username}` : " "} />

                {notice ? <Notice text={notice.text} tone={notice.tone} onClose={() => setNotice(null)} /> : null}

                <section className={CARD} aria-label="Thông tin cá nhân">
                    <h2 className={SECTION_TITLE}>Thông tin cá nhân</h2>
                    <dl className="grid gap-x-6 gap-y-4 sm:grid-cols-2">
                        <Field label="Họ tên" value={me?.name} />
                        <Field label="Tên đăng nhập" value={me?.username} />
                        <Field label="Vai trò" value={roleText} />
                        {me?.studentCode ? <Field label="Mã sinh viên" value={me.studentCode} /> : null}
                        {me?.classCode ? <Field label="Lớp" value={me.classCode} /> : null}
                        <Field label="Giới tính" value={me?.profile.gender ? GENDER_LABEL[me.profile.gender] : null} />
                        <Field label="Ngày sinh" value={formatDate(me?.profile.birthDate ?? null)} />
                        <Field label="Tình trạng" value={me?.profile.status ? STATUS_LABEL[me.profile.status] : null} />
                        <Field label="Nơi sinh" value={me?.profile.birthplace} />
                        <Field label="Quốc tịch" value={me?.profile.nationality} />
                        <Field label="CCCD" value={me?.profile.cccdMasked} />
                        <Field label="Dân tộc" value={me ? (me.profile.hasEthnicity ? "Đã khai" : "Chưa khai") : null} />
                        <Field label="Tôn giáo" value={me ? (me.profile.hasReligion ? "Đã khai" : "Chưa khai") : null} />
                    </dl>
                    <p className={HELP}>Các mục trên do quản trị viên quản lý. Muốn sửa, liên hệ phòng đào tạo.</p>
                </section>

                <section className={CARD} aria-label="Liên hệ">
                    <h2 className={SECTION_TITLE}>Liên hệ</h2>
                    <form onSubmit={saveContact} className="grid gap-4 sm:grid-cols-2">
                        <TextField
                            id={phoneId}
                            label="Số điện thoại"
                            value={phone}
                            onChange={(e) => setPhone(e.target.value)}
                            placeholder="Ví dụ: 0901234567"
                        />
                        <div className="sm:col-span-2">
                            <TextField
                                id={addressId}
                                label="Địa chỉ"
                                value={address}
                                onChange={(e) => setAddress(e.target.value)}
                            />
                        </div>
                        <div className="flex flex-wrap gap-2 sm:col-span-2">
                            <Button type="submit" disabled={busy}>
                                Lưu liên hệ
                            </Button>
                        </div>
                    </form>
                </section>

                <section className={CARD} aria-label="Bảo mật">
                    <h2 className={SECTION_TITLE}>Bảo mật</h2>
                    <div className="flex flex-col gap-3 text-sm text-ink">
                        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-line pb-3 last:border-b-0 last:pb-0">
                            <span>Mật khẩu</span>
                            <Link href="/change-password" className={buttonClasses("secondary", "", "sm")}>
                                Đổi mật khẩu
                            </Link>
                        </div>
                        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-line pb-3 last:border-b-0 last:pb-0">
                            <span>
                                Xác thực hai lớp:{" "}
                                {isAdmin ? (hasMfa() ? "đã bật" : "chưa bật") : "chỉ áp dụng cho quản trị viên"}
                            </span>
                            {isAdmin ? (
                                <Link href="/security/2fa" className={buttonClasses("secondary", "", "sm")}>
                                    Quản lý 2FA
                                </Link>
                            ) : null}
                        </div>
                        <div className="flex flex-col gap-2 last:pb-0">
                            <div className="flex flex-wrap items-center justify-between gap-3">
                                <span>Đăng xuất tất cả thiết bị</span>
                                <Button variant="danger" size="sm" disabled={busy} onClick={signOutEverywhere}>
                                    Đăng xuất mọi nơi
                                </Button>
                            </div>
                            <p className={HELP}>Kết thúc mọi phiên đăng nhập, kể cả trên thiết bị này. Bạn sẽ phải đăng nhập lại.</p>
                        </div>
                    </div>
                </section>
            </main>
        </div>
    );
}

function Field({ label, value }: { label: string; value: string | null | undefined }) {
    return (
        <div className="flex flex-col gap-0.5">
            <dt className={HELP}>{label}</dt>
            <dd className="break-words text-sm text-ink">{show(value)}</dd>
        </div>
    );
}