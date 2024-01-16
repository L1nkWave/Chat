"use client";

import "./parallax.css";

import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/dist/ScrollTrigger";
import Image from "next/image";
import { useLayoutEffect, useRef } from "react";

import { CustomButton } from "@/components/CustomButton/CustomButton";

export function Parallax() {
  const parallaxRef = useRef(null);
  const stroke = useRef(null);
  const frontWave = useRef(null);
  const middleWave = useRef(null);
  const backWave = useRef(null);
  const link = useRef(null);
  const copy = useRef(null);
  const btn = useRef(null);

  useLayoutEffect(() => {
    const ctx = gsap.context(() => {
      gsap.registerPlugin(ScrollTrigger);
      const tl = gsap.timeline({
        defaults: { duration: 1 },
        scrollTrigger: {
          trigger: parallaxRef.current,
          start: "top top",
          end: "5000 bottom",
          scrub: true,
          pin: true,
        },
      });
      tl.to(
        frontWave.current,
        {
          x: "-=75%",
        },
        0
      );
      tl.to(
        middleWave.current,
        {
          x: "-=85%",
        },
        0
      );
      tl.to(
        stroke.current,
        {
          opacity: 0,
        },
        -0.1
      );
      tl.to(
        stroke.current,
        {
          x: "+=64%",
        },
        0
      );
      tl.to(
        backWave.current,
        {
          x: "+=65%",
        },
        0
      );
      tl.to(
        link.current,
        {
          y: "+=100%",
          opacity: 0,
        },
        0
      );
      tl.to(
        copy.current,
        {
          y: "-=150%",
          opacity: 1,
        },
        0
      );
      tl.to(
        btn.current,
        {
          opacity: 1,
        },
        1
      );
    });
    return () => ctx.revert();
  }, []);

  return (
    <div className="overflow-hidden">
      <div ref={parallaxRef} className="parallax">
        <Image
          width={500}
          height={500}
          ref={stroke}
          src="/images/parallax/stroke.svg"
          className="stroke"
          alt="stroke"
        />
        <Image
          width={500}
          height={500}
          ref={frontWave}
          alt="frontwave"
          className="front-wave"
          src="/images/parallax/frontwave.svg"
        />
        <Image
          width={500}
          height={500}
          ref={middleWave}
          src="/images/parallax/middlewave.svg"
          className="middle-wave"
          alt="middlewave"
        />
        <Image
          width={500}
          height={500}
          ref={backWave}
          className="back-wave"
          src="/images/parallax/backwave.svg"
          alt="backwave"
        />

        <Image
          width={500}
          height={500}
          ref={link}
          src="/images/parallax/link.svg"
          className="link"
          alt="link"
        />

        <div ref={copy} className="parallax-text">
          <h1 className="font-semibold">Link Wave Chat</h1>
          <CustomButton className="opacity-0" ref={btn}>
            Join
          </CustomButton>
        </div>
      </div>
    </div>
  );
}
