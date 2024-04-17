import { ButtonHTMLAttributes, DetailedHTMLProps, PropsWithChildren } from "react";

import { IconName } from "@/components/Icon/Icon";

export type CustomButtonVariant = "square" | "flattened" | "primary" | "transparent" | "outline";

export type CustomButtonProps = {
  iconSize?: number;
  variant?: CustomButtonVariant;
  icon?: IconName;
} & PropsWithChildren<DetailedHTMLProps<ButtonHTMLAttributes<HTMLButtonElement>, HTMLButtonElement>>;
