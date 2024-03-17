import React, { forwardRef, Ref } from "react";

import { BUTTON_VARIANT_STYLES } from "@/components/CustomButton/customButton.config";
import { CustomButtonProps } from "@/components/CustomButton/customButton.types";
import { Icon } from "@/components/Icon/Icon";

export const CustomButton = forwardRef(
  ({ className, children, variant, icon, iconSize, ...props }: CustomButtonProps, ref: Ref<HTMLButtonElement>) => {
    let style: string;
    switch (variant) {
      case "transparent":
        style = BUTTON_VARIANT_STYLES.transparent;
        break;
      case "flattened":
        style = BUTTON_VARIANT_STYLES.flattened;
        break;
      case "square":
        style = BUTTON_VARIANT_STYLES.square;
        break;
      default:
        style = BUTTON_VARIANT_STYLES.primary;
        break;
    }

    return (
      <button
        type="button"
        className={`outline-none flex gap-2 justify-center items-center ${style} ${className}`}
        ref={ref}
        {...props}
      >
        {icon ? <Icon name={icon} iconSize={iconSize} /> : null}
        {children ?? null}
      </button>
    );
  }
);
