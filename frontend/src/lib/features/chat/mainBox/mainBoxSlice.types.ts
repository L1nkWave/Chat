export type MainBoxState = "chat" | "user-info" | "empty";

export type MainBoxStateParams = {
  currentMainBox: MainBoxState;
};
