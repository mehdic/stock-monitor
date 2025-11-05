package com.stockmonitor.integration;

import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Data feed retry handler with exponential backoff (T208). */
@Component
@Slf4j
public class DataFeedRetryHandler {

  public <T> T executeWithRetry(Supplier<T> operation, int maxRetries) {
    int attempt = 0;
    while (attempt < maxRetries) {
      try {
        return operation.get();
      } catch (Exception e) {
        attempt++;
        if (attempt >= maxRetries) throw e;
        long backoff = (long) Math.pow(2, attempt) * 1000;
        log.warn("Retry attempt {} after {} ms", attempt, backoff);
        try {
          Thread.sleep(backoff);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(ie);
        }
      }
    }
    throw new RuntimeException("Max retries exceeded");
  }
}
