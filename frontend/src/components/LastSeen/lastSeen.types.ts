export type TextColors = "text-blue-300" | "text-gray-400";

export type LastSeenProps = {
  online: boolean;
  lastSeen: number;
  className?: string;
  iconSize?: number;
  textColor?: TextColors;
};
