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
        className={`bg-black text-white px-8 py-4 mt-3 font-bold rounded-lg ${className}`}
        ref={ref}
        {...props}
      >
        {children}
      </button>
    );
  }
);
