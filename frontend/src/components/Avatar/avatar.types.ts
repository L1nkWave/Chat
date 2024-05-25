import type { ImageProps } from "next/dist/shared/lib/get-img-props";

export type ItemParams = {
  id: number | string;
  avatarPath?: string;
};

export type AvatarProps = {
  item: ItemParams;
  status?: boolean;
  online?: boolean;
  statusClassName?: string;
} & Omit<ImageProps, "src">;
