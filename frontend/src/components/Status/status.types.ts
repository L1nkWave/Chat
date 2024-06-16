import { DetailedHTMLProps, HTMLAttributes } from "react";

export type StatusProps = {
  textStatus?: boolean;
  classNameTextContainer?: string;
  online: boolean;
} & DetailedHTMLProps<HTMLAttributes<HTMLDivElement>, HTMLDivElement>;
