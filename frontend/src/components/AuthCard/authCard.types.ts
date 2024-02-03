import {
  DetailedHTMLProps,
  FormHTMLAttributes,
  PropsWithChildren,
} from "react";

export type AuthCardProps = DetailedHTMLProps<
  FormHTMLAttributes<HTMLFormElement>,
  HTMLFormElement
> &
  PropsWithChildren;
