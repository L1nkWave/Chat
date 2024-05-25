import Image from "next/image";

import { AvatarProps } from "@/components/Avatar/avatar.types";
import { Status } from "@/components/Status/Status";
import { defaultUserAvatar } from "@/helpers/defaultUserAvatar";

export function Avatar({ item, statusClassName, className, online, status = true, ...props }: AvatarProps) {
  const useId = parseInt(item.id as string, 10);
  return (
    <span className="relative flex h-full">
      <Image
        className={`object-cover rounded-full ${className}`}
        width={64}
        height={64}
        src={defaultUserAvatar(useId)}
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
