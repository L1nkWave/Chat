"use client";

import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/dist/ScrollTrigger";
import Image from "next/image";
import React, { useLayoutEffect, useRef } from "react";

import { CustomButton } from "@/components/CustomButton/CustomButton";
import { ThemeToggle } from "@/components/ThemeToggle/ThemeToggle";

export function Header() {
  const parallaxRef = useRef(null);
  useLayoutEffect(() => {
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
  }, []);

  return (
    <div className="header">
      <header className="fixed w-full z-10">
        <div className="px-16 flex items-center justify-between p-4 relative">
          <div className="flex items-center relative">
            <div ref={parallaxRef}>
              <button type="button">
                <div className="flex rounded-lg contrast-100 relative">
                  <div className="filter absolute top-0 left-0 w-full h-full blur rounded-full opacity-35" />
                  <Image
                    width={55}
                    height={55}
                    src="/images/logo.svg"
                    alt="Logo"
                    className="rounded-2xl"
                  />
                </div>
              </button>
            </div>
          </div>

          <nav className="flex gap-6 items-center text-sm font-medium text-gray-800">
            <ThemeToggle />
            <CustomButton>Sign In</CustomButton>
          </nav>
        </div>
      </header>
    </div>
  );
}
