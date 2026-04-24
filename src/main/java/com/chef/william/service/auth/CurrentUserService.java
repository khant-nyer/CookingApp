package com.chef.william.service.auth;

import com.chef.william.exception.UnauthorizedException;
import com.chef.william.model.AccountStatus;
import com.chef.william.model.User;
import com.chef.william.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    @Transactional
    public User getRequiredCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UnauthorizedException("Missing or invalid authentication token");
        }

        String sub = jwt.getClaimAsString("sub");
        if (sub == null || sub.isBlank()) {
            throw new UnauthorizedException("Access token is missing 'sub' claim");
        }

        User user = userRepository.findByCognitoSub(sub)
                .orElseThrow(() -> new UnauthorizedException("No application user found for the authenticated token"));

        syncVerificationStateFromToken(user, jwt);
        return user;
    }

    private void syncVerificationStateFromToken(User user, Jwt jwt) {
        Object rawEmailVerified = jwt.getClaim("email_verified");
        if (rawEmailVerified == null) {
            return;
        }

        boolean tokenEmailVerified = user.isEmailVerified();
        if (rawEmailVerified instanceof Boolean value) {
            tokenEmailVerified = value;
        } else if (rawEmailVerified instanceof String value) {
            tokenEmailVerified = Boolean.parseBoolean(value);
        }

        boolean changed = user.isEmailVerified() != tokenEmailVerified;
        if (changed) {
            user.setEmailVerified(tokenEmailVerified);
        }

        if (tokenEmailVerified && user.getAccountStatus() == AccountStatus.PENDING_EMAIL_VERIFICATION) {
            user.setAccountStatus(AccountStatus.ACTIVE);
            changed = true;
        }

        if (changed) {
            userRepository.save(user);
        }
    }
}
