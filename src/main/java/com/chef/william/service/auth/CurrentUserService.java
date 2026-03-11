package com.chef.william.service.auth;

import com.chef.william.exception.UnauthorizedException;
import com.chef.william.model.User;
import com.chef.william.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getRequiredCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UnauthorizedException("Missing or invalid authentication token");
        }

        String sub = jwt.getClaimAsString("sub");
        if (sub == null || sub.isBlank()) {
            throw new UnauthorizedException("Access token is missing 'sub' claim");
        }

        return userRepository.findByCognitoSub(sub)
                .orElseThrow(() -> new UnauthorizedException("No application user found for the authenticated token"));
    }
}
