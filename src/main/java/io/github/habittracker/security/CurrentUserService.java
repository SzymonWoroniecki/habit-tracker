package io.github.habittracker.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public String getUserId() {
        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        return auth != null ? auth.getName() : null;
    }
}