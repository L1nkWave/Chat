import { HTMLAttributes, PropsWithChildren, ReactElement } from "react";

export type CustomButtonVariant = "flattened" | "primary" | "transparent";

export type CustomButtonProps = {
  variant?: CustomButtonVariant;
  icon?: ReactElement;
} & PropsWithChildren<HTMLAttributes<HTMLButtonElement>>;
