/**
 * The access token lives only in memory. It is never written to localStorage or
 * sessionStorage, so an XSS payload cannot read it after the tab is closed.
 * The refresh token stays in an httpOnly cookie that JavaScript cannot read.
 */
let accessToken: string | null = null;

export function getAccessToken(): string | null {
    return accessToken;
}

export function setAccessToken(token: string | null): void {
    accessToken = token;
}