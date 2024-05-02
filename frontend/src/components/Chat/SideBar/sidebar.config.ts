import { SidebarItem } from "@/components/Chat/SideBar/sidebar.types";

export const SIDEBAR_ITEM: SidebarItem = {
  variant: "square",
  iconSize: 42,
  buttons: {
    chat: {
      icon: "chat-outline",
    },
    "add-chat": {
      icon: "add-chat-outline",
    },
    "find-people": {
      icon: "find-people-outline",
    },
    contact: {
      icon: "group-outline",
    },
    setting: {
      icon: "setting-outline",
    },
  },
};
