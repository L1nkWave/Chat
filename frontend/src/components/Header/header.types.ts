import { ReactElement } from "react";

export type HeaderProps = {
  withoutEffects?: boolean;
  logoLabel?: string;
};

export type AuthButtonParams = {
  icon: ReactElement;
  label: string;
};

export type HeaderStateParams = {
  authButton: AuthButtonParams;
};
