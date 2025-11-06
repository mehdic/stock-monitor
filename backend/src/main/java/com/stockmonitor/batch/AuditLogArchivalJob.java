package com.stockmonitor.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Audit log archival job with 7-year retention (T232.5). */
@Component
@Slf4j
public class AuditLogArchivalJob {

  @Scheduled(cron = "0 0 0 1 * *") // First of month
  public void verifyRetentionPolicy() {
    log.info("Verifying 7-year audit log retention policy");
    // TODO: Query for records >7 years, should be empty due to TimescaleDB retention
  }
}
