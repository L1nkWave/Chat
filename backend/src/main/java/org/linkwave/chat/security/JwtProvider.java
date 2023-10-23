package org.linkwave.chat.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import org.linkwave.chat.entity.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Optional;

import static com.auth0.jwt.algorithms.Algorithm.HMAC256;
import static org.linkwave.chat.security.Token.ACCESS;

@Component
public class JwtProvider {

    @Value("${jwt.subject}")
    private String subject;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.exp.access}")
    private int accessExpiration;

    @Value("${jwt.exp.refresh}")
    private int refreshExpiration;

    @Value("${jwt.secret-key.access}")
    private String accessSecretKey;

    @Value("${jwt.secret-key.refresh}")
    private String refreshSecretKey;

    public String generateToken(UserEntity user, Token token) {

        Instant now = Instant.now();
        long amountToAdd;
        TemporalUnit unit;

        if (token.equals(ACCESS)) {
            amountToAdd = accessExpiration;
            unit = ChronoUnit.MINUTES;
        } else {
            amountToAdd = refreshExpiration;
            unit = ChronoUnit.DAYS;
        }

        return JWT.create()
                .withSubject(subject)
                .withIssuer(issuer)
                .withClaim("id", user.getId())
                .withClaim("username", user.getUsername())
                .withClaim("name", user.getName())
                .withIssuedAt(now)
                .withExpiresAt(now.plus(amountToAdd, unit))
                .sign(HMAC256(token.equals(ACCESS) ? accessSecretKey : refreshSecretKey));
    }

    public Optional<DecodedJWT> decode(String token, Token type) {
        JWTVerifier verifier = JWT.require(HMAC256(type.equals(ACCESS) ? accessSecretKey : refreshSecretKey))
                .withSubject(subject)
                .withIssuer(issuer)
                .withClaimPresence("id")
                .withClaimPresence("username")
                .withClaimPresence("name")
                .build();
        try {
            return Optional.of(verifier.verify(token));
        } catch (JWTVerificationException e) {
            return Optional.empty();
        }
    }

}