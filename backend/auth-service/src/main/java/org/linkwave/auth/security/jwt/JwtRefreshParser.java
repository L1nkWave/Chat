package org.linkwave.auth.security.jwt;

import com.auth0.jwt.algorithms.Algorithm;
import org.linkwave.shared.auth.Token;
import org.linkwave.shared.auth.TokenParser;

import java.security.interfaces.RSAPublicKey;

public class JwtRefreshParser implements TokenParser {

    private final Algorithm algorithm;

    public JwtRefreshParser(RSAPublicKey publicKey) {
        this.algorithm = Algorithm.RSA256(publicKey, null);
    }

    @Override
    public Token parse(String token) {
        return parse(token, algorithm);
    }

}
