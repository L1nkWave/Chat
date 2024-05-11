import * as Icons from "@public/icons";

import { DoubleCheckIconProps } from "@/components/DoubleCheckIcon/doubleCheckIcon.types";
import { DEFAULT_ICON_SIZE } from "@/components/Icon/icon.settings";

export function DoubleCheckIcon({
  containerClassName,
  iconSize,
  cutCheckIconColor,
  checkIconColor,
}: DoubleCheckIconProps) {
  const divisor = 2.8;
  let iconShift: number;

  if (iconSize) {
    iconShift = iconSize / divisor;
  } else {
    iconShift = DEFAULT_ICON_SIZE / divisor;
  }
  return (
    <div className={`relative ml-1 ${!!containerClassName && containerClassName}`}>
      <div
        className="absolute"
        style={{ width: iconSize ?? DEFAULT_ICON_SIZE, right: iconShift, color: cutCheckIconColor }}
      >
        <Icons.CutCheckIcon />
      </div>
      <div style={{ width: iconSize ?? DEFAULT_ICON_SIZE, color: checkIconColor }}>
        <Icons.CheckIcon />
      </div>
    </div>
  );
}
