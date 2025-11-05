package com.stockmonitor.dto;

import com.stockmonitor.model.User;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
  private UUID id;
  private String email;
  private String firstName;
  private String lastName;
  private String baseCurrency;
  private User.UserRole role;
  private Boolean emailVerified;
  private LocalDateTime disclaimerAcceptedAt;
  private LocalDateTime createdAt;
  private LocalDateTime lastLoginAt;
  private String timezone;

  public static UserDTO from(User user) {
    return UserDTO.builder()
        .id(user.getId())
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .baseCurrency(user.getBaseCurrency())
        .role(user.getRole())
        .emailVerified(user.getEmailVerified())
        .disclaimerAcceptedAt(user.getDisclaimerAcceptedAt())
        .createdAt(user.getCreatedAt())
        .lastLoginAt(user.getLastLoginAt())
        .timezone(user.getTimezone())
        .build();
  }
}
