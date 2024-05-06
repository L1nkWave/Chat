package org.linkwave.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.auth.repository.DeactivatedTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleaner {

    private final DeactivatedTokenRepository tokenRepository;

    /**
     *   Cleans all added tokens to database in specified interval.<br/>
     *   Fixed rate as 61 was chosen in order to wait of refresh token expiration.
     */
    @Transactional
    @Scheduled(timeUnit = TimeUnit.MINUTES, fixedRate = 61)
    public void clean() {
        log.debug("-> clean()");
        tokenRepository.removeAllExpiredTokens();
    }

}
