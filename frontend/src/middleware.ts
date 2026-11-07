import { NextResponse, type NextRequest } from "next/server";

// Mỗi request có một nonce mới. Chỉ script có nonce đúng mới được chạy.
export function middleware(request: NextRequest) {
    const nonce = btoa(crypto.randomUUID());
    const isDev = process.env.NODE_ENV !== "production";

    const csp = [
        "default-src 'self'",
        `script-src 'self' 'nonce-${nonce}' 'strict-dynamic'${isDev ? " 'unsafe-eval'" : ""}`,
        "style-src 'self' 'unsafe-inline'",
        "img-src 'self' data: blob:",
        "connect-src 'self' ws: wss:",
        "font-src 'self'",
        "object-src 'none'",
        "base-uri 'self'",
        "form-action 'self'",
        "frame-ancestors 'none'",
    ].join("; ");

    const requestHeaders = new Headers(request.headers);
    requestHeaders.set("Content-Security-Policy", csp);
    requestHeaders.set("x-nonce", nonce);

    const response = NextResponse.next({ request: { headers: requestHeaders } });
    response.headers.set("Content-Security-Policy", csp);
    return response;
}

export const config = {
    matcher: [{ source: "/((?!_next/static|_next/image|favicon.ico|brand/).*)" }],
};