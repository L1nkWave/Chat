import { useId, type ComponentProps } from "react";

type TextFieldProps = ComponentProps<"input"> & {
    label: string;
    error?: string;
    hint?: string;
};

export function TextField({
                              label,
                              error,
                              hint,
                              id,
                              className = "",
                              ...props
                          }: TextFieldProps) {
    const generatedId = useId();
    const inputId = id ?? generatedId;
    const hintId = `${inputId}-hint`;
    const errorId = `${inputId}-error`;

    const describedBy =
        [error ? errorId : null, hint && !error ? hintId : null]
            .filter(Boolean)
            .join(" ") || undefined;

    return (
        <div className="flex flex-col gap-1.5">
            <label htmlFor={inputId} className="text-sm font-medium text-ink">
                {label}
            </label>
            <input
                id={inputId}
                aria-invalid={error ? true : undefined}
                aria-describedby={describedBy}
                className={`h-11 w-full rounded-lg border bg-surface px-3 text-base text-ink outline-none transition-colors placeholder:text-muted focus-visible:ring-2 focus-visible:ring-sky disabled:opacity-60 ${
                    error ? "border-danger" : "border-line focus:border-primary"
                } ${className}`}
                {...props}
            />
            {hint && !error ? (
                <p id={hintId} className="text-xs text-muted">
                    {hint}
                </p>
            ) : null}
            {error ? (
                <p id={errorId} role="alert" className="text-xs text-danger">
                    {error}
                </p>
            ) : null}
        </div>
    );
}