import React, { forwardRef, Ref } from "react";

import { styles } from "@/components/CustomButton/customButton.config";
import { CustomButtonProps } from "@/components/CustomButton/customButton.types";

export const CustomButton = forwardRef(
  (
    { className, children, variant, icon, ...props }: CustomButtonProps,
    ref: Ref<HTMLButtonElement>
  ) => {
    let style: string;
    switch (variant) {
      case "transparent":
        style = styles.transparent;
        break;
      case "flattened":
        style = styles.flattened;
        break;
      default:
        style = styles.primary;
        break;
    }
    return (
      <button
        type="button"
        className={`flex gap-2 justify-center ${style} ${className}`}
        ref={ref}
        {...props}
      >
        {icon ? <span className="w-[24px]">{icon}</span> : null}
        {children}
      </button>
    );
  }
);
