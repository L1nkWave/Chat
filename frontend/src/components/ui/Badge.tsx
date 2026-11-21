import type { ReactNode } from "react";

import { badgeStyle, type BadgeTone } from "./styles";

export function Badge({ tone = "neutral", children }: { tone?: BadgeTone; children: ReactNode }) {
    return <span className={badgeStyle(tone)}>{children}</span>;
}
