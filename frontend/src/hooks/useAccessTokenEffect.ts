import { useRouter } from "next/navigation";
import { DependencyList, EffectCallback, useEffect } from "react";

import { useAppSelector } from "@/lib/hooks";

export function useAccessTokenEffect(callback: EffectCallback, dependencies: DependencyList) {
  const { accessToken } = useAppSelector(state => state.auth);
  const router = useRouter();

  useEffect(() => {
    if (!accessToken) {
      router.push("/sign-in");
      return;
    }
    callback();
  }, [accessToken, router, callback, dependencies]);
}
