import React, {
  DetailedHTMLProps,
  InputHTMLAttributes,
  ReactElement,
} from "react";

export type CustomInputProps = {
  icon?: ReactElement;
  label?: string;
  error?: string | false;
  containerClassName?: string;
} & DetailedHTMLProps<InputHTMLAttributes<HTMLInputElement>, HTMLInputElement>;

export function CustomInput({
  icon,
  containerClassName,
  label,
  error,
  className,
  ...props
}: Readonly<CustomInputProps>) {
  return (
    <div className={`mb-6 ${containerClassName}`}>
      <label
        htmlFor="custom-input"
        className="block mb-2 text-sm font-medium text-gray-900 dark:text-blue-200"
      >
        {label}
        <div className="flex items-center w-full py-1 px-2 text-gray-900 border-2 border-gray-500 rounded-lg focus:border-gray-400">
          <span className="w-[32px] mr-2">{icon}</span>
          <input
            type="text"
            className={`block w-full bg-transparent outline-none sm:text-md dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-blue-500 ${className}`}
            {...props}
          />
        </div>
        {error ?? null}
      </label>
    </div>
  );
}
