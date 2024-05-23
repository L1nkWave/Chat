"use client";

import { useTheme } from "next-themes";
import React, { useEffect, useState } from "react";
import { DarkModeSwitch } from "react-toggle-dark-mode";

import { DARK_THEME, LIGHT_THEME } from "@/components/ThemeToggle/themeToggle.config";

export function ThemeToggle() {
  const { setTheme, resolvedTheme } = useTheme();
  const [isLightTheme, setIsLightTheme] = useState(LIGHT_THEME === resolvedTheme);

  useEffect(() => {
    setIsLightTheme(LIGHT_THEME === resolvedTheme);
  }, [resolvedTheme]);

  const handleToggleTheme = () => {
    const newTheme = isLightTheme ? DARK_THEME : LIGHT_THEME;
    setTheme(newTheme);
    setIsLightTheme(!isLightTheme);
  };

  return (
    <DarkModeSwitch
      className="text-4xl"
      moonColor="#000"
      sunColor="#fff"
      checked={isLightTheme}
      onChange={handleToggleTheme}
      size={42}
    />
  );
}
