import React from "react";

import { Header } from "@/components/Header/Header";
import { SignInForm } from "@/components/SignInForm/SignInForm";

export default function SignIn() {
  return (
    <>
      <Header withoutEffects logoLabel="LinkWave" />
      <div className="flex flex-col items-center justify-center p-24 h-[100vh]">
        <SignInForm />
      </div>
    </>
  );
}
