import { DetailedHTMLProps, InputHTMLAttributes } from "react";

import { IconName } from "@/components/Icon/Icon";

export type CustomInputProps = {
  icon?: IconName;
  label?: string;
  error?: string | false;
  containerClassName?: string;
} & DetailedHTMLProps<InputHTMLAttributes<HTMLInputElement>, HTMLInputElement>;
