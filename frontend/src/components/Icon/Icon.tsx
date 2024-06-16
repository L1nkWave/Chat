import React, { DetailedHTMLProps, HTMLAttributes } from "react";

import { DEFAULT_ICON_SIZE, iconMapping } from "@/components/Icon/icon.settings";

export type IconName = keyof typeof iconMapping;
export type IconProps = {
  name: IconName;
  iconSize?: number;
} & DetailedHTMLProps<HTMLAttributes<HTMLSpanElement>, HTMLSpanElement>;

export function Icon({ name, iconSize, color, ...props }: Readonly<IconProps>) {
  const SelectedIcon = iconMapping[name];

  return (
    <span className="flex" style={{ width: iconSize ?? DEFAULT_ICON_SIZE, color }} {...props}>
      <SelectedIcon />
    </span>
  );
}
