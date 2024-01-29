import { DetailedHTMLProps, InputHTMLAttributes, ReactElement } from "react";

export type CustomInputProps = {
  icon?: ReactElement;
  label?: string;
  error?: string | false;
  containerClassName?: string;
} & DetailedHTMLProps<InputHTMLAttributes<HTMLInputElement>, HTMLInputElement>;
