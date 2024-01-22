import "@theme-toggles/react/css/Expand.css";

import { Expand } from "@theme-toggles/react";
import { useTheme } from "next-themes";
import React, { useEffect } from "react";

export function ThemeToggle() {
  useEffect(() => {}, []);
  const { setTheme, resolvedTheme } = useTheme();

  return (
    <Expand
      className="text-4xl text-dark-400 dark:text-gray-200"
      placeholder="Change Theme"
      toggled={resolvedTheme === "light"}
      onToggle={() => setTheme(resolvedTheme === "light" ? "dark" : "light")}
    />
  );
}
