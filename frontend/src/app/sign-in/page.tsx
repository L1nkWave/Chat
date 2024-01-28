import { FormIcon, LockIcon, PinIcon } from "@public/icons";
import React from "react";

import { AuthCard } from "@/components/AuthCard/AuthCard";
import { CustomButton } from "@/components/CustomButton/CustomButton";
import { CustomInput } from "@/components/CustomInput/CustomInput";
import { Header } from "@/components/Header/Header";

export default function Login() {
  return (
    <>
      <Header withoutEffects logoLabel="LinkWave" />
      <main className="flex flex-col items-center justify-center p-24 h-[100vh]">
        <AuthCard>
          <div className="w-[100px] bg-dark-150 rounded-full p-4 text-2xl text-gray-100">
            <FormIcon />
          </div>
          <h2 className="text-white text-2xl">Log in existing account</h2>
          <span className="text-gray-300">
            Enter your credentials and log in
          </span>
          <div className="mt-10 flex flex-col w-full items-center">
            <CustomInput
              placeholder="@emmtlor"
              label="Username"
              className="text-base"
              containerClassName="w-3/5"
              icon={<PinIcon />}
            />
            <CustomInput
              placeholder="●●●●●●●●●"
              type="password"
              className="text-base"
              label="Password"
              containerClassName="w-3/5"
              icon={<LockIcon />}
            />
          </div>
          <CustomButton className="mt-10 w-3/5" variant="flattened">
            Sign In
          </CustomButton>
        </AuthCard>
      </main>
    </>
  );
}
