export type DecodedToken = {
  iss: string;
  sub: string;
  iat: number;
  exp: number;
  "token-id": string;
  "user-id": number;
  authorities: string[];
};
