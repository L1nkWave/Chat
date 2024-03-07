import { InteractiveListStateParams } from "@/lib/features/chat/interactiveList/interactiveListSlice.types";

export const INTERACTIVE_LIST_SLICE_NAME = "interactiveList";
export const interactiveListInitialState: InteractiveListStateParams = {
  currentInteractiveList: "chats",
};
