import { isToday } from "date-fns/isToday";

import { Icon } from "@/components/Icon/Icon";
import { LastSeenProps } from "@/components/LastSeen/lastSeen.types";
import { formatDate } from "@/helpers/FormatDate/formatDate";
import { DateFormat } from "@/helpers/FormatDate/formatDate.types";
import { UserStatus } from "@/lib/features/user/userSlice.types";

export function LastSeen({
  online,
  lastSeen,
  className,
  textColor = "text-gray-400",
  iconSize = 16,
}: Readonly<LastSeenProps>) {
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
      <span className={`${textColor} ${className}`}>
        {online ? (
          UserStatus.ONLINE
        ) : (
          <span className="flex items-center">
            <Icon name="time-outline" iconSize={iconSize} className="mr-1" /> {lastSeenDate}
          </span>
        )}
      </span>
    </div>
  );
}
