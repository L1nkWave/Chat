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

    @Transactional
    @Scheduled(timeUnit = TimeUnit.MINUTES, fixedRate = 10)
    public void clean() {
        log.debug("-> clean()");
        tokenRepository.removeAllExpiredTokens();
    }

}
