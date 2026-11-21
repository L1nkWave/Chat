import type { ButtonHTMLAttributes } from "react";

import { buttonStyle, type ButtonSize, type ButtonVariant } from "./styles";

/** "outline" là tên cũ của "secondary", giữ lại để các trang đăng nhập không phải sửa. */
type Variant = ButtonVariant | "outline";

function normalize(variant: Variant): ButtonVariant {
    return variant === "outline" ? "secondary" : variant;
}

/** Class cho phần tử giống nút nhưng là liên kết, ví dụ <Link>. */
export function buttonClasses(variant: Variant = "primary", extra = "", size: ButtonSize = "md"): string {
    return buttonStyle(normalize(variant), size, extra);
}

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
    variant?: Variant;
    size?: ButtonSize;
};

export function Button({ variant = "primary", size = "md", className = "", type = "button", ...rest }: ButtonProps) {
    return <button type={type} className={buttonClasses(variant, className, size)} {...rest} />;
}
