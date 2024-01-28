"use client";

import { CurvedArrowIcon, UserPlusIcon } from "@public/icons";
import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/dist/ScrollTrigger";
import Image from "next/image";
import { usePathname, useRouter } from "next/navigation";
import React, { useEffect, useLayoutEffect, useRef, useState } from "react";

import { CustomButton } from "@/components/CustomButton/CustomButton";
import {
  HeaderProps,
  HeaderStateParams,
} from "@/components/Header/header.types";
import { ThemeToggle } from "@/components/ThemeToggle/ThemeToggle";

export function Header({
  withoutEffects = false,
  logoLabel,
}: Readonly<HeaderProps>) {
  const pathname = usePathname();
  const onSignInPage = pathname === "/sign-in";

  const [headerState, setHeaderState] = useState<HeaderStateParams>({
    authButton: {
      label: "Sign In",
      icon: <CurvedArrowIcon />,
    },
  });

  const router = useRouter();
  const parallaxRef = useRef(null);

  useEffect(() => {
    if (onSignInPage) {
      setHeaderState({
        authButton: {
          label: "Sign Up",
          icon: <UserPlusIcon />,
        },
      });
    }
  }, [onSignInPage]);

  useLayoutEffect(() => {
    if (withoutEffects) return () => {};
    const ctx = gsap.context(() => {
      gsap.registerPlugin(ScrollTrigger);

      const tl = gsap.timeline({
        defaults: { duration: 1 },
        scrollTrigger: {
          trigger: parallaxRef.current,
          start: "top",
          end: "5500 bottom",
          scrub: true,
        },
      });

      tl.fromTo(
        parallaxRef.current,
        { opacity: 0, y: 0 },
        { opacity: 1, y: "+=5%" }
      );
    });

    return () => ctx.revert();
  }, [withoutEffects]);

  const handleRedirectAuth = () => {
    const path = pathname === "/sign-in" ? "/sign-up" : "/sign-in";
    router.push(path);
  };

  const handleRedirectHome = () => {
    router.push("/");
  };

  return (
    <div className="header">
      <header className="fixed w-full z-10">
        <div className="px-16 flex items-center justify-between p-4 relative">
          <div
            ref={parallaxRef}
            className={`relative ${!withoutEffects ? "opacity-0" : "opacity-1"}`}
          >
            <button
              className="flex items-center"
              type="button"
              onClick={handleRedirectHome}
            >
              <div className="flex rounded-lg contrast-100 relative">
                <div className="filter absolute top-0 left-0 w-full h-full blur rounded-full items-center" />
                <Image
                  width={55}
                  height={55}
                  src="/images/logo.svg"
                  alt="Logo"
                  className="rounded-2xl"
                />
              </div>
              <h3 className="text-2xl font-bold pl-6 text-dark-400 dark:text-white">
                {logoLabel}
              </h3>
            </button>
          </div>

          <nav className="flex gap-6 items-center text-sm font-medium text-gray-800">
            <ThemeToggle />
            <CustomButton
              icon={headerState.authButton.icon}
              className="flex gap-2 text-lg"
              variant="transparent"
              onClick={handleRedirectAuth}
            >
              {headerState.authButton.label}
            </CustomButton>
          </nav>
        </div>
      </header>
    </div>
  );
}
