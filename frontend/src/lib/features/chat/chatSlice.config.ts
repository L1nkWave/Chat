import { ListStateEnum, MainBoxStateEnum } from "@/components/Chat/chat.types";
import { ChatStateParams } from "@/lib/features/chat/chatSlice.types";

export const CHAT_SLICE_NAME = "chat";
export const chatInitialState: ChatStateParams = {
  currentMainBoxState: MainBoxStateEnum.EMPTY,
  currentInteractiveListState: ListStateEnum.CHATS,
};
