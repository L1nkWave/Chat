import { FormEventHandler, PropsWithChildren } from "react";

import { IconName } from "@/components/Icon/Icon";

export type FormProps = {
  titleIcon: IconName;
  title: string;
  description: string;
  buttonTitle: string;
  onSubmit?: FormEventHandler<HTMLFormElement>;
} & PropsWithChildren;
