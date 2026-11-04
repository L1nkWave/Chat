import { isToday } from "date-fns/isToday";

import { Icon } from "@/components/Icon/Icon";
import { LastSeenProps } from "@/components/LastSeen/lastSeen.types";
// eslint-disable-next-line import/named
import { formatDate } from "@/helpers/FormatDate/formatDate";
import { DateFormat } from "@/helpers/FormatDate/formatDate.types";
import { toDate } from "@/helpers/FormatDate/toDate";
import { UserStatus } from "@/lib/features/user/userSlice.types";

export function LastSeen({
  online,
  className,
  lastSeen,
  textColor = "text-gray-400",
  iconSize = 16,
}: Readonly<LastSeenProps>) {
  let lastSeenDate;

  if (!online) {
    const convertedLastSeen = toDate(lastSeen);
    if (isToday(convertedLastSeen)) {
      lastSeenDate = `Today ${formatDate(convertedLastSeen, DateFormat.HOURS_AND_MINUTES)}`;
    } else {
      lastSeenDate = formatDate(convertedLastSeen, DateFormat.EUROPEAN);
    }
  }
  return (
    <div className="flex flex-row">
      <span className={`${textColor} ${className}`}>
        {online ? (
          <span className="flex items-center text-gray-300">{UserStatus.ONLINE}</span>
        ) : (
          <span className="flex items-center">
            <Icon name="time-outline" iconSize={iconSize} className="mr-1" /> {lastSeenDate}
          </span>
        )}
      </span>
    </div>
  );
}
