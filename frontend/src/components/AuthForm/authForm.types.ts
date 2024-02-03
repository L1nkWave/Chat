import { FormEventHandler, PropsWithChildren, ReactElement } from "react";

export type AuthFormProps = {
  titleIcon: ReactElement;
  title: string;
  description: string;
  buttonTitle: string;
  onSubmit?: FormEventHandler<HTMLFormElement>;
} & PropsWithChildren;
