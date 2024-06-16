import { DetailedHTMLProps, InputHTMLAttributes } from "react";

import { IconName } from "@/components/Icon/Icon";

export type CustomInputProps = {
  icon?: IconName;
  label?: string;
  error?: string | false;
  innerContainerClassName?: string;
  containerClassName?: string;
  iconSize?: number;
  reverseIconPosition?: boolean;
} & DetailedHTMLProps<InputHTMLAttributes<HTMLInputElement>, HTMLInputElement>;
