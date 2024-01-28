"use client";

import "@theme-toggles/react/css/Expand.css";

import { Expand } from "@theme-toggles/react";
import { useTheme } from "next-themes";
import React, { useEffect, useState } from "react";

import {
  DARK_THEME,
  LIGHT_THEME,
} from "@/components/ThemeToggle/themeToggle.config";

export function ThemeToggle() {
  const { setTheme, resolvedTheme } = useTheme();
  const [isLightTheme, setIsLightTheme] = useState(true);

  useEffect(() => {
    setIsLightTheme(resolvedTheme === LIGHT_THEME);
  }, [resolvedTheme]);

  return (
    <Expand
      className="text-4xl text-dark-400 dark:text-gray-200"
      placeholder="Change Theme"
      toggled={isLightTheme}
      onToggle={() => setTheme(isLightTheme ? DARK_THEME : LIGHT_THEME)}
    />
  );
}
