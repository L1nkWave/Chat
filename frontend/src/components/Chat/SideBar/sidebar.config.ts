import { SidebarItem } from "@/components/Chat/SideBar/sidebar.types";

export const SIDEBAR_ITEM: SidebarItem = {
  variant: "square",
  iconSize: 42,
  buttons: {
    chats: {
      icon: "chat-outline",
    },
    "add-chat": {
      icon: "add-chat-outline",
    },
    "find-contacts": {
      icon: "find-contacts-outline",
    },
    contacts: {
      icon: "group-outline",
    },
    setting: {
      icon: "setting-outline",
    },
  },
};
