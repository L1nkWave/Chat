import Image from "next/image";

import { AvatarProps } from "@/components/Avatar/avatar.types";
import { Status } from "@/components/Status/Status";
import { defaultUserAvatar } from "@/helpers/defaultUserAvatar";

export function Avatar({
  width,
  height,
  item,
  src,
  statusClassName,
  className,
  online,
  status = true,
  ...props
}: AvatarProps) {
  const avatarSrc = !src && item ? defaultUserAvatar(parseInt(item.id as string, 10)) : src;
  return (
    <span className="relative flex h-full rounded-full">
      <Image
        width={width}
        height={height}
        className={`object-cover rounded-full ${className}`}
        src={avatarSrc!}
        {...props}
      />
      {status && online && (
        <Status
          className={`absolute bottom-1.5 left-12 transform translate-x-1/2 translate-y-1/2 ${statusClassName ?? statusClassName}`}
          online={online}
        />
      )}
    </span>
  );
}
