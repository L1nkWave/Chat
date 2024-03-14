import React from "react";

import { DEFAULT_ICON_SIZE, iconMapping } from "@/components/Icon/icon.settings";

export type IconName = keyof typeof iconMapping;
export interface IconProps {
  name: IconName;
  iconSize?: number;
}

export function Icon({ name, iconSize }: Readonly<IconProps>) {
  const SelectedIcon = iconMapping[name];

  return (
    <span style={{ width: iconSize ?? DEFAULT_ICON_SIZE }}>
      <SelectedIcon />
    </span>
  );
}
