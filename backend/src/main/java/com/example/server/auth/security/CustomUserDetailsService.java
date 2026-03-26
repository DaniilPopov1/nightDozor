package com.example.server.auth.security;

import com.example.server.auth.entity.User;
import com.example.server.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
/**
 * Адаптер между доменной моделью пользователя и механизмом аутентификации Spring Security.
 */
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    /**
     * Загружает пользователя по email и преобразует его в {@link UserDetails}.
     *
     * @param username email пользователя
     * @return объект пользователя для Spring Security
     * @throws UsernameNotFoundException если пользователь не найден
     */
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .disabled(!user.isEnabled())
                .accountLocked(!user.isAccountNonLocked())
                .build();
    }
}
