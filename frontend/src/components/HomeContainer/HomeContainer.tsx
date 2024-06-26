"use client";

import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/dist/ScrollTrigger";
import React, { useEffect, useRef, useState } from "react";

import { HomeContainerProps } from "@/components/HomeContainer/homeContainer.types";
import { COLORS } from "@/constants/colors";

export function HomeContainer({ children }: Readonly<HomeContainerProps>) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [background, setBackground] = useState(`${COLORS.blue["100"]}70`);

  useEffect(() => {
    const ctx = gsap.context(() => {
      gsap.registerPlugin(ScrollTrigger);
      gsap.timeline({
        scrollTrigger: {
          trigger: containerRef.current,
          start: "1000 top",
          end: "4000 top",
          scrub: true,
          onUpdate: self => {
            const newColor = gsap.utils.interpolate(`${COLORS.blue["100"]}65`, COLORS.blue["700"], self.progress);
            setBackground(newColor);
          },
        },
      });
    });
    return () => ctx.revert();
  }, []);

  return (
    <div className="bg-blue-500/80">
      <div className="bg-blue-600/60">
        <div
          ref={containerRef}
          className="px-32 text-white pb-16 flex flex-col gap-40"
          style={{
            background: `${background}`,
          }}
        >
          {children}
        </div>
      </div>
    </div>
  );
}
