import { IconName } from "@/components/Icon/Icon";

export type FormParams = {
  title: string;
  description: string;
  titleIcon: IconName;
  buttonTitle: string;
};

export type InputParams = {
  type?: string;
  placeholder: string;
  name: string;
  label: string;
  icon: IconName;
};
