import { ChatStateParams } from "@/lib/features/chat/chatSlice.types";

export const CHAT_SLICE_NAME = "chat";
export const chatInitialState: ChatStateParams = {
  currentMainBoxState: "empty",
  currentInteractiveListState: "chats",
};
