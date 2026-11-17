package org.linkwave.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.linkwave.auth.dto.TokensDto;
import org.linkwave.auth.entity.User;
import org.linkwave.auth.repository.DeactivatedTokenRepository;
import org.linkwave.auth.repository.UserRepository;
import org.linkwave.auth.security.DefaultUserDetails;
import org.linkwave.auth.security.jwt.AccessTokenFactory;
import org.linkwave.auth.security.jwt.AccessTokenFactoryImpl;
import org.linkwave.auth.security.jwt.JwtRefreshSerializer;
import org.linkwave.auth.security.jwt.RefreshTokenFactory;
import org.linkwave.auth.security.jwt.RefreshTokenFactoryImpl;
import org.linkwave.auth.security.totp.ChallengeService;
import org.linkwave.auth.service.TwoFactorService;
import org.linkwave.shared.auth.JwtAccessParser;
import org.linkwave.shared.auth.JwtAccessSerializer;
import org.linkwave.shared.auth.Token;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;

import static org.linkwave.auth.security.utils.Cookies.createRefreshCookie;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * API 2FA. setup, confirm, disable yêu cầu access token của ADMIN/ROOT (kiểm tra trong controller).
 * verify nhận challenge từ bước đăng nhập và mã TOTP, rồi mới cấp token đầy đủ.
 */
@RestController
@RequestMapping("/api/v1/auth/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private final JwtAccessParser accessParser;
    private final DeactivatedTokenRepository deactivatedTokens;
    private final JwtRefreshSerializer refreshSerializer;
    private final JwtAccessSerializer accessSerializer;
    private final TwoFactorService twoFactor;
    private final ChallengeService challenges;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;

    private final RefreshTokenFactory refreshTokenFactory = new RefreshTokenFactoryImpl();
    private final AccessTokenFactory accessTokenFactory = new AccessTokenFactoryImpl();

    public record CodeRequest(@NotBlank String code) {
    }

    public record VerifyRequest(@NotBlank String challenge, @NotBlank String code) {
    }

    public record ConfirmResponse(List<String> recoveryCodes) {
    }

    @PostMapping("/setup")
    public TwoFactorService.SetupResponse setup(HttpServletRequest request) {
        final Token token = requirePrivilegedToken(request);
        return twoFactor.setup(token.userId(), token.username());
    }

    @PostMapping("/confirm")
    public ConfirmResponse confirm(@RequestBody @Valid CodeRequest body, HttpServletRequest request) {
        final Token token = requirePrivilegedToken(request);
        return new ConfirmResponse(twoFactor.confirm(token.userId(), body.code()));
    }

    @PostMapping("/disable")
    @ResponseStatus(NO_CONTENT)
    public void disable(@RequestBody @Valid CodeRequest body, HttpServletRequest request) {
        final Token token = requirePrivilegedToken(request);
        requireCheck(twoFactor.disable(token.userId(), body.code()));
    }

    @PostMapping("/verify")
    public ResponseEntity<TokensDto> verify(@RequestBody @Valid VerifyRequest body) {
        final Long userId = challenges.verify(body.challenge());
        requireCheck(twoFactor.verifyLogin(userId, body.code()));

        final String username = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Tài khoản không tồn tại"))
                .getUsername();
        final DefaultUserDetails details = (DefaultUserDetails) userDetailsService.loadUserByUsername(username);
        if (!details.isEnabled()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Tài khoản đã bị khóa");
        }

        final var authentication = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        final Token refreshToken = org.linkwave.auth.security.utils.TokenAuthorities.withMfa(
                refreshTokenFactory.build(authentication), true);
        final Token accessToken = accessTokenFactory.build(refreshToken);
        final ResponseCookie cookie = createRefreshCookie(
                refreshSerializer.serialize(refreshToken),
                Duration.between(refreshToken.createdAt(), refreshToken.expireAt()));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokensDto(refreshToken.expireAt(), accessSerializer.serialize(accessToken),
                        userRepository.findById(userId).map(User::isMustChangePassword).orElse(false)));
    }

    private Token requirePrivilegedToken(HttpServletRequest request) {
        final String header = request.getHeader(AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ResponseStatusException(UNAUTHORIZED, "Thiếu access token");
        }
        final Token token = accessParser.parse(header.substring(7));
        if (token == null || deactivatedTokens.existsById(token.id())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Access token không hợp lệ");
        }
        final boolean privileged = token.authorities().contains("ROLE_ADMIN")
                || token.authorities().contains("ROLE_ROOT");
        if (!privileged) {
            throw new ResponseStatusException(FORBIDDEN, "Chỉ ADMIN hoặc ROOT được dùng 2FA");
        }
        return token;
    }

    private static void requireCheck(TwoFactorService.Check check) {
        switch (check) {
            case OK -> { }
            case LOCKED -> throw new ResponseStatusException(TOO_MANY_REQUESTS,
                    "Nhập sai quá nhiều lần, thử lại sau 5 phút");
            case INVALID -> throw new ResponseStatusException(UNAUTHORIZED, "Mã xác thực không đúng");
        }
    }

}