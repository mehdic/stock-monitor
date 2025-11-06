package com.stockmonitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Audit logging service (T232). */
@Service
@Slf4j
public class AuditLogService {

  public void logSecurityEvent(String userId, String action, String details) {
    log.info("AUDIT: user={}, action={}, details={}", userId, action, details);
    // TODO: Persist to audit log table
  }
}
