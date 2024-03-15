import type { ImageProps } from "next/dist/shared/lib/get-img-props";

export type ItemParams = {
  id: number;
  avatarPath?: string;
};

export type AvatarProps = {
  item: ItemParams;
  status?: boolean;
  online?: boolean;
} & Omit<ImageProps, "src">;
