package com.stockmonitor.security;

import com.stockmonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
    // identifier is now user ID (UUID string) from JWT token
    var user =
        userRepository
            .findById(java.util.UUID.fromString(identifier))
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));

    // Return UserDetails with user ID as username (consistent with JWT token)
    return new org.springframework.security.core.userdetails.User(
        user.getId().toString(),
        user.getPasswordHash(),
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
  }
}
