import React from "react";

import { CustomInputProps } from "@/components/CustomInput/customInput.types";

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
      <label htmlFor="custom-input" className="block mb-2 text-sm font-medium text-blue-600 dark:text-blue-200">
        {label}
        <div className="flex items-center w-full py-1 px-2 text-gray-900 border-2 border-gray-500 rounded-lg focus:border-gray-400">
          <span className="w-[32px] mr-2">{icon}</span>
          <input
            type="text"
            className={`block w-full h-full bg-transparent outline-none text-blue-100 sm:text-md dark:placeholder-gray-400 dark:text-blue-500 ${className}`}
            {...props}
          />
        </div>
        <span className="text-red-100 dark:text-red-50">{error ?? null}</span>
      </label>
    </div>
  );
}
