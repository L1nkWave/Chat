import type { ImageProps } from "next/dist/shared/lib/get-img-props";

export type ItemParams = {
  id: number | string;
  avatarPath?: string;
  avatarAvailable?: boolean;
};

export type AvatarProps = {
  preview?: string;
  isGroupAvatar?: boolean;
  defaultAvatar?: string;
  item: ItemParams;
  status?: boolean;
  online?: boolean;
  isAvatarAvailable?: boolean;
  statusClassName?: string;
} & Omit<ImageProps, "src">;
