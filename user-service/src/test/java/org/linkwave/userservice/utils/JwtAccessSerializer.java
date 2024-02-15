package org.linkwave.userservice.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.lang.NonNull;

import java.security.interfaces.RSAPrivateKey;

public class JwtAccessSerializer {

    private final String issuer;
    private final Algorithm algorithm;

    public JwtAccessSerializer(String issuer, RSAPrivateKey privateKey) {
        this.issuer = issuer;
        this.algorithm = Algorithm.RSA256(null, privateKey);
    }

    public String serialize(@NonNull Token token) {
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(token.username())
                .withIssuedAt(token.createdAt())
                .withExpiresAt(token.expireAt())
                .withClaim("token-id", token.id().toString())
                .withClaim("user-id", token.userId())
                .withClaim("authorities", token.authorities())
                .sign(algorithm);
    }

}
