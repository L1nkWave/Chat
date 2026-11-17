import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { isPasswordChangeRequired } from "@/lib/auth/session";
import { ensureAccessToken, hasMfa, isAdminUser, isRootUser } from "@/lib/auth/token";

export type GateStatus = "loading" | "ready" | "forbidden";

/**
 * Kiểm tra phiên đăng nhập khi mở trang. Chưa đăng nhập thì chuyển về /login.
 * adminOnly: chỉ ADMIN hoặc ROOT mới được vào.
 * allowPasswordChange: chỉ dùng cho trang đổi mật khẩu. Các trang khác sẽ chuyển sang đó nếu tài khoản bắt buộc đổi.
 */
export function useAuthGate(
    adminOnly: boolean,
    requireMfa = false,
    allowPasswordChange = false,
): { status: GateStatus; isRoot: boolean } {
    const router = useRouter();
    const [status, setStatus] = useState<GateStatus>("loading");
    const [isRoot, setIsRoot] = useState(false);

    useEffect(() => {
        let alive = true;
        void ensureAccessToken().then((signedIn) => {
            if (!alive) return;
            if (!signedIn) {
                router.replace("/login");
                return;
            }
            if (!allowPasswordChange && isPasswordChangeRequired()) {
                router.replace("/change-password");
                return;
            }
            if (adminOnly && !isAdminUser()) {
                setStatus("forbidden");
                return;
            }
            if (requireMfa && isAdminUser() && !hasMfa()) {
                // ADMIN/ROOT chưa bật 2FA: đưa đến trang thiết lập trước
                router.replace("/security/2fa?required=1");
                return;
            }
            setIsRoot(isRootUser());
            setStatus("ready");
        });
        return () => {
            alive = false;
        };
    }, [adminOnly, requireMfa, allowPasswordChange, router]);

    return { status, isRoot };
}