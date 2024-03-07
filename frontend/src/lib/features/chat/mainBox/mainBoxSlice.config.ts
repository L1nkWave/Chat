import { MainBoxStateParams } from "@/lib/features/chat/mainBox/mainBoxSlice.types";

export const MAIN_BOX_SLICE_NAME = "mainBox";
export const mainBoxInitialState: MainBoxStateParams = {
  currentMainBox: "empty",
};
