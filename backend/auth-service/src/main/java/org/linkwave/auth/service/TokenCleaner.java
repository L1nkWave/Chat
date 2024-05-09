package org.linkwave.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.auth.repository.DeactivatedTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.HOURS;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleaner {

    private final DeactivatedTokenRepository tokenRepository;

    /**
     *   Cleans all added tokens to database in specified interval.
     */
    @Transactional
    @Scheduled(timeUnit = TimeUnit.MINUTES, fixedRate = 30)
    public void clean() {
        log.debug("-> clean()");
        final Instant hourAgo = Instant.now().minus(1L, HOURS);
        tokenRepository.removeAllExpiredTokens(hourAgo);
    }

}
