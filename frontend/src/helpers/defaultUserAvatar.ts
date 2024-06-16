import * as Avatar1 from "@public/avatars/user/avatar1.png";
import * as Avatar2 from "@public/avatars/user/avatar2.png";
import * as Avatar3 from "@public/avatars/user/avatar3.png";
import * as Avatar4 from "@public/avatars/user/avatar4.png";
import * as Avatar5 from "@public/avatars/user/avatar5.png";
import * as Avatar6 from "@public/avatars/user/avatar6.png";

export const defaultUserAvatar = (userId: number): string => {
  const avatars = [Avatar1, Avatar2, Avatar3, Avatar4, Avatar5, Avatar6];
  const avatar = avatars[userId % avatars.length];

  return avatar.default.src;
};
