import { InteractiveListVariant } from "@/components/Chat/InteractiveList/interactiveList.types";
import { MainBoxVariant } from "@/components/Chat/MainBox/mainBox.types";

export type ChatStateParams = {
  currentMainBoxState: MainBoxVariant;
  currentInteractiveListState: InteractiveListVariant;
};
