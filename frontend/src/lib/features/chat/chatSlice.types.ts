export type MainBoxStateParams = "chat" | "user-info" | "empty" | "settings";
export type UserItemStateParams = "contact" | "chat";

export type ChatStateParams = {
  currentMainBox: MainBoxStateParams;
  currentUserItem: UserItemStateParams;
};
