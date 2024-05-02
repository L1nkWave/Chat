import { CustomButtonVariant } from "@/components/CustomButton/customButton.types";
import { IconName } from "@/components/Icon/Icon";

export type SidebarButtons = {
  onClick?: () => void;
  icon: IconName;
};

export type SidebarButtonName = "chat" | "add-chat" | "find-people" | "contact" | "setting";

export type SidebarItem = {
  iconSize: number;
  variant: CustomButtonVariant;
  buttons: Record<SidebarButtonName, SidebarButtons>;
};
