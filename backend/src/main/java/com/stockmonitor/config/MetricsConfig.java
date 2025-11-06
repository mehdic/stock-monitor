package com.stockmonitor.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

  @Bean
  public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
    return registry ->
        registry
            .config()
            .commonTags("application", "stockmonitor", "environment", "production");
  }

  @Bean
  public TimedAspect timedAspect(MeterRegistry registry) {
    return new TimedAspect(registry);
  }

  /**
   * Custom metrics for recommendation engine performance
   *
   * <p>Usage in services:
   *
   * <pre>
   * Timer.Sample sample = Timer.start(registry);
   * // ... perform recommendation calculation
   * sample.stop(registry.timer("recommendation.calculation.time",
   *     "status", "success", "universe", universeId));
   * </pre>
   */
  public static class RecommendationMetrics {
    public static final String CALCULATION_TIME = "recommendation.calculation.time";
    public static final String DATA_FRESHNESS_CHECK = "recommendation.data.freshness.check";
    public static final String FACTOR_SCORE_RETRIEVAL = "recommendation.factor.score.retrieval";
    public static final String CONSTRAINT_EVALUATION = "recommendation.constraint.evaluation";
    public static final String OPTIMIZATION_TIME = "recommendation.optimization.time";
  }

  /**
   * Custom metrics for cache operations
   *
   * <p>Usage in services:
   *
   * <pre>
   * registry.counter("cache.hit", "cache", "factorScores").increment();
   * registry.counter("cache.miss", "cache", "factorScores").increment();
   * </pre>
   */
  public static class CacheMetrics {
    public static final String HIT = "cache.hit";
    public static final String MISS = "cache.miss";
    public static final String EVICTION = "cache.eviction";
  }

  /**
   * Custom metrics for data source operations
   *
   * <p>Usage in services:
   *
   * <pre>
   * registry.counter("datasource.fetch.count",
   *     "source", "yahooFinance", "status", "success").increment();
   * registry.timer("datasource.fetch.time",
   *     "source", "yahooFinance").record(duration);
   * </pre>
   */
  public static class DataSourceMetrics {
    public static final String FETCH_COUNT = "datasource.fetch.count";
    public static final String FETCH_TIME = "datasource.fetch.time";
    public static final String ERROR_COUNT = "datasource.error.count";
  }

  /**
   * Custom metrics for portfolio operations
   *
   * <p>Usage in services:
   *
   * <pre>
   * registry.gauge("portfolio.total.value", portfolio, Portfolio::getMarketValue);
   * registry.gauge("portfolio.cash", portfolio, Portfolio::getCash);
   * </pre>
   */
  public static class PortfolioMetrics {
    public static final String TOTAL_VALUE = "portfolio.total.value";
    public static final String CASH = "portfolio.cash";
    public static final String HOLDING_COUNT = "portfolio.holding.count";
    public static final String PNL = "portfolio.pnl";
  }

  /**
   * Custom metrics for backtest operations
   *
   * <p>Usage in services:
   *
   * <pre>
   * registry.timer("backtest.execution.time",
   *     "strategy", strategyName, "status", "completed")
   *     .record(duration);
   * </pre>
   */
  public static class BacktestMetrics {
    public static final String EXECUTION_TIME = "backtest.execution.time";
    public static final String SHARPE_RATIO = "backtest.sharpe.ratio";
    public static final String MAX_DRAWDOWN = "backtest.max.drawdown";
    public static final String TOTAL_RETURN = "backtest.total.return";
  }
}
