import { isToday } from "date-fns/isToday";

import { Icon } from "@/components/Icon/Icon";
import { LastSeenProps } from "@/components/LastSeen/lastSeen.types";
import { formatDate } from "@/helpers/FormatDate/formatDate";
import { DateFormat } from "@/helpers/FormatDate/formatDate.types";
import { UserStatus } from "@/lib/features/user/userSlice.types";

export function LastSeen({ online, lastSeen }: Readonly<LastSeenProps>) {
  let lastSeenDate;
  if (typeof lastSeen === "number") {
    if (isToday(lastSeen)) {
      lastSeenDate = `Today ${formatDate(lastSeen, DateFormat.HOURS_AND_MINUTES)}`;
    } else {
      lastSeenDate = formatDate(lastSeen, DateFormat.EUROPEAN);
    }
  }
  return (
    <div className="flex flex-row">
      <span className="text-sm text-gray-400">
        {online ? (
          UserStatus.ONLINE
        ) : (
          <span className="flex items-center">
            <Icon name="time-outline" iconSize={16} className="mr-1" /> {lastSeenDate}
          </span>
        )}
      </span>
    </div>
  );
}
