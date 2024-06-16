import { useRouter } from "next/navigation";
import { DependencyList, EffectCallback, useEffect, useLayoutEffect } from "react";

import { useAppSelector } from "@/lib/hooks";

export function useAccessTokenEffect(callback: EffectCallback, dependencies: DependencyList) {
  const { accessToken } = useAppSelector(state => state.user);
  const router = useRouter();

  useEffect(() => {
    if (!accessToken) {
      router.push("/sign-in");
      return;
    }
    callback();
  }, [accessToken, router, ...dependencies]);
}

export function useAccessTokenLayoutEffect(callback: EffectCallback, dependencies: DependencyList) {
  const { accessToken } = useAppSelector(state => state.user);
  const router = useRouter();

  useLayoutEffect(() => {
    if (!accessToken) {
      router.push("/sign-in");
      return;
    }
    callback();
  }, [accessToken, router, ...dependencies]);
}
