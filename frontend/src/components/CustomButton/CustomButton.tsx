import React, { forwardRef, Ref } from "react";

import { CustomButtonProps } from "@/components/CustomButton/customButton.types";

export const CustomButton = forwardRef(
  (
    { className, children, ...props }: CustomButtonProps,
    ref: Ref<HTMLButtonElement>
  ) => {
    return (
      <button
        type="button"
        className={`text-white bg-dark-400 px-8 py-4 font-bold rounded-lg dark:bg-gray-100 dark:text-dark-400 ${className}`}
        ref={ref}
        {...props}
      >
        {children}
      </button>
    );
  }
);
