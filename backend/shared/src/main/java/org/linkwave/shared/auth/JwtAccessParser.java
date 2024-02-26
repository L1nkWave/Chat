package org.linkwave.shared.auth;

import com.auth0.jwt.algorithms.Algorithm;

import java.security.interfaces.RSAPublicKey;

public class JwtAccessParser implements TokenParser {

    private final Algorithm algorithm;

    public JwtAccessParser(RSAPublicKey publicKey) {
        this.algorithm = Algorithm.RSA256(publicKey, null);
    }

    @Override
    public Token parse(String token) {
        return parse(token, algorithm);
    }

}
