package com.stockmonitor.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Data feed scheduler (T209-T215). */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataFeedScheduler {

  @Scheduled(cron = "0 0 18 * * MON-FRI") // 6 PM weekdays
  public void fetchEndOfDayPrices() {
    log.info("Fetching end-of-day prices");
    // TODO: Implement
  }

  @Scheduled(cron = "0 0 2 1 * *") // 2 AM first of month
  public void fetchFundamentalData() {
    log.info("Fetching fundamental data");
    // TODO: Implement
  }

  @Scheduled(cron = "0 0 3 * * MON") // 3 AM Mondays
  public void fetchFactorData() {
    log.info("Fetching factor data");
    // TODO: Implement
  }

  @Scheduled(cron = "0 30 17 * * MON-FRI") // 5:30 PM weekdays
  public void fetchBenchmarkData() {
    log.info("Fetching benchmark data");
    // TODO: Implement
  }

  @Scheduled(cron = "0 0 9 * * *") // 9 AM daily
  public void fetchFxRates() {
    log.info("Fetching FX rates");
    // TODO: Implement
  }
}
