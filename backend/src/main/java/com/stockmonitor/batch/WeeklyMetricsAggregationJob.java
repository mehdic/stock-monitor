package com.stockmonitor.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Weekly metrics aggregation (T236.4). */
@Component
@Slf4j
public class WeeklyMetricsAggregationJob {

  @Scheduled(cron = "0 0 0 * * SUN") // Sunday 00:00 UTC
  public void aggregateMetrics() {
    log.info("Aggregating weekly metrics");
    // TODO: Aggregate recommendation confidence, turnover, run duration, staleness incidents
  }
}
