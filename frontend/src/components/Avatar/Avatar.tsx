import Image from "next/image";
import { useEffect, useState } from "react";

import { getAvatar, getGroupAvatar } from "@/api/http/user/user";
import { AvatarProps } from "@/components/Avatar/avatar.types";
import { Status } from "@/components/Status/Status";
import { defaultUserAvatar } from "@/helpers/defaultUserAvatar";

export function Avatar({
  defaultAvatar,
  preview,
  width,
  height,
  isGroupAvatar,
  item,
  statusClassName,
  className,
  online,
  status = true,
  ...props
}: AvatarProps) {
  const [avatarSrc, setAvatarSrc] = useState<string>("/src");

  function blobToBase64(blob: Blob): Promise<string> {
    return new Promise(resolve => {
      const reader = new FileReader();
      reader.onloadend = () => resolve(reader.result as string);
      reader.readAsDataURL(blob);
    });
  }

  // eslint-disable-next-line sonarjs/cognitive-complexity
  useEffect(() => {
    const fetchImage = async () => {
      try {
        if (item.avatarPath || item.avatarAvailable) {
          const file = isGroupAvatar ? await getGroupAvatar(item.id as string) : await getAvatar(item.id as string);
          const blob = new Blob([file], { type: "image/png" });

          const base64Image = await blobToBase64(blob);
          setAvatarSrc(base64Image);
        } else if (item?.id) {
          if (defaultAvatar) {
            setAvatarSrc(defaultAvatar);
          } else {
            setAvatarSrc(defaultUserAvatar(parseInt(item.id as string, 10)));
          }
        }
      } catch (error) {
        if (item?.id) {
          if (defaultAvatar) {
            setAvatarSrc(defaultAvatar);
          } else {
            setAvatarSrc(defaultUserAvatar(parseInt(item.id as string, 10)));
          }
        }
      }
    };
    fetchImage();
  }, [item]);

  return (
    <span className="relative flex rounded-full">
      <Image
        width={width}
        height={height}
        style={{ maxWidth: width, maxHeight: height, minWidth: width, minHeight: height }}
        className={`inline-block rounded-full object-fill ${className}`}
        src={(preview || avatarSrc) ?? "/src"}
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
