import { useId } from "react";
import type { InputHTMLAttributes } from "react";

import { FIELD, FIELD_INVALID, HELP, LABEL } from "./styles";

type TextFieldProps = InputHTMLAttributes<HTMLInputElement> & {
    label: string;
    error?: string;
    hint?: string;
};

/** Trường nhập có nhãn luôn hiện phía trên, gợi ý và lỗi gắn với ô qua aria-describedby. */
export function TextField({ label, error, hint, id, className = "", ...rest }: TextFieldProps) {
    const generatedId = useId();
    const inputId = id ?? generatedId;
    const errorId = `${inputId}-error`;
    const hintId = `${inputId}-hint`;

    const describedBy = error ? errorId : hint ? hintId : undefined;

    return (
        <div className="flex flex-col gap-1.5">
            <label htmlFor={inputId} className={LABEL}>
                {label}
            </label>
            <input
                id={inputId}
                aria-invalid={error ? true : undefined}
                aria-describedby={describedBy}
                className={`${FIELD} ${error ? FIELD_INVALID : ""} ${className}`}
                {...rest}
            />
            {hint && !error ? (
                <p id={hintId} className={HELP}>
                    {hint}
                </p>
            ) : null}
            {error ? (
                <p id={errorId} role="alert" className="text-[13px] leading-relaxed text-danger">
                    {error}
                </p>
            ) : null}
        </div>
    );
}
