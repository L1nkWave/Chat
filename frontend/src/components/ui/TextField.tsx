import { useId } from "react";
import type { InputHTMLAttributes } from "react";

type TextFieldProps = InputHTMLAttributes<HTMLInputElement> & {
    label: string;
    error?: string;
    hint?: string;
};

export function TextField({ label, error, hint, id, className = "", ...rest }: TextFieldProps) {
    const generatedId = useId();
    const inputId = id ?? generatedId;
    const errorId = `${inputId}-error`;
    const hintId = `${inputId}-hint`;

    const describedBy = error ? errorId : hint ? hintId : undefined;

    return (
        <div className="flex flex-col gap-2">
            <label htmlFor={inputId} className="text-sm font-medium text-ink">
                {label}
            </label>
            <input
                id={inputId}
                aria-invalid={error ? true : undefined}
                aria-describedby={describedBy}
                className={`h-11 rounded-md border bg-paper px-3.5 text-[15px] text-ink outline-none transition-colors placeholder:text-muted focus:border-primary focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary ${
                    error ? "border-danger" : "border-line"
                } ${className}`}
                {...rest}
            />
            {hint && !error ? (
                <p id={hintId} className="text-[13px] leading-relaxed text-muted">
                    {hint}
                </p>
            ) : null}
            {error ? (
                <p id={errorId} role="alert" className="text-[13px] text-danger">
                    {error}
                </p>
            ) : null}
        </div>
    );
}