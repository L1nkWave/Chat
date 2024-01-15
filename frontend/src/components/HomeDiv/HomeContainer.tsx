"use client";

import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/dist/ScrollTrigger";
import React, { useEffect, useRef, useState } from "react";

import { HomeContainerProps } from "@/components/HomeDiv/homeContainer.types";
import { colors } from "@/utils/colors";

export function HomeContainer({ children }: Readonly<HomeContainerProps>) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [background, setBackground] = useState(colors["link-wave"].underwater);

  useEffect(() => {
    const ctx = gsap.context(() => {
      gsap.registerPlugin(ScrollTrigger);
      gsap.timeline({
        scrollTrigger: {
          trigger: containerRef.current,
          start: "1000 top",
          end: "5000 top",
          scrub: true,
          onUpdate: self => {
            const newColor = gsap.utils.interpolate(
              colors["link-wave"].underwater,
              colors["link-wave"].deepwater,
              self.progress
            );
            setBackground(newColor);
          },
        },
      });
    });

    return () => ctx.revert();
  }, []);

  return (
    <div
      ref={containerRef}
      className="px-40 bg-link-wave-underwater text-white"
      style={{
        background: `${background}`,
      }}
    >
      {children}
    </div>
  );
}
