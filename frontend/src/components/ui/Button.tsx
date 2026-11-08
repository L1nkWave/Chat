import type { ButtonHTMLAttributes } from "react";

type ButtonVariant = "primary" | "outline";

const base =
    "inline-flex h-11 items-center justify-center rounded-md px-5 text-[15px] font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:cursor-not-allowed disabled:opacity-50";

const variants: Record<ButtonVariant, string> = {
    primary: "bg-primary text-primary-ink hover:bg-primary-hover",
    outline: "border border-line bg-surface text-ink hover:bg-tint",
};

/** Classes for elements that must look like a button but are links, e.g. <Link>. */
export function buttonClasses(variant: ButtonVariant = "primary", extra = ""): string {
    return `${base} ${variants[variant]} ${extra}`.trim();
}

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
    variant?: ButtonVariant;
};

export function Button({ variant = "primary", className = "", type = "button", ...rest }: ButtonProps) {
    return <button type={type} className={buttonClasses(variant, className)} {...rest} />;
}