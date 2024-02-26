package org.linkwave.ws.config;

import org.linkwave.shared.auth.JwtAccessParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.converter.RsaKeyConverters;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class SecurityConfiguration {

    private static final String PUBLIC_KEY_PATH = "classpath:keys/access_public_key.pem";

    @Bean
    public JwtAccessParser jwtAccessParser(ResourceLoader loader) throws IOException {
        try (var resource = loader.getResource(PUBLIC_KEY_PATH).getInputStream()) {
            final RSAPublicKey publicKey = RsaKeyConverters.x509().convert(resource);
            return new JwtAccessParser(publicKey);
        }
    }

}
