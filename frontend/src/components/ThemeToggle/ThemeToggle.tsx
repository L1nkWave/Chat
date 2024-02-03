"use client";

import "@theme-toggles/react/css/Expand.css";

import dynamic from "next/dynamic";
import { useTheme } from "next-themes";
import React, { useState } from "react";

import {
  DARK_THEME,
  LIGHT_THEME,
} from "@/components/ThemeToggle/themeToggle.config";

const Expand = dynamic(
  () => import("@theme-toggles/react").then(module => module.Expand),
  { ssr: false }
);

export function ThemeToggle() {
  const { setTheme, resolvedTheme } = useTheme();
  const [isLightTheme, setIsLightTheme] = useState(
    LIGHT_THEME === resolvedTheme
  );

  const handleToggleTheme = () => {
    setIsLightTheme(!isLightTheme);
    setTheme(isLightTheme ? DARK_THEME : LIGHT_THEME);
  };

  return (
    <Expand
      className="text-4xl text-dark-400 dark:text-gray-200"
      placeholder="Change Theme"
      toggled={isLightTheme}
      onToggle={handleToggleTheme}
    />
  );
}
