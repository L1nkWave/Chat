export enum UserStatus {
  ONLINE = "Connected",
  OFFLINE = "Disconnected",
}
export type UserStateParams = {
  accessToken: string | null;
};
