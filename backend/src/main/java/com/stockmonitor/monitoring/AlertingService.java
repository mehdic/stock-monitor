package com.stockmonitor.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Alerting service for system monitoring (T236). */
@Service
@Slf4j
public class AlertingService {

  public void sendAlert(String severity, String message) {
    log.warn("ALERT [{}]: {}", severity, message);
    // TODO: Send to alerting system (PagerDuty, etc.)
  }
}
