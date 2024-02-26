import {
  ButtonHTMLAttributes,
  DetailedHTMLProps,
  PropsWithChildren,
  ReactElement,
} from "react";

export type CustomButtonVariant =
  | "square"
  | "flattened"
  | "primary"
  | "transparent";

export type CustomButtonProps = {
  iconSize?: number;
  variant?: CustomButtonVariant;
  icon?: ReactElement;
} & PropsWithChildren<
  DetailedHTMLProps<ButtonHTMLAttributes<HTMLButtonElement>, HTMLButtonElement>
>;
