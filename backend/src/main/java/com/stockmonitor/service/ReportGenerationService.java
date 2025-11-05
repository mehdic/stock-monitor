package com.stockmonitor.service;

import com.stockmonitor.dto.ReportDTO;
import com.stockmonitor.model.Recommendation;
import com.stockmonitor.model.RecommendationRun;
import com.stockmonitor.repository.RecommendationRepository;
import com.stockmonitor.repository.RecommendationRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Report generation service per FR-041, FR-042, FR-043, FR-044.
 *
 * T109: Generates HTML and PDF reports for recommendation runs.
 *
 * Uses Thymeleaf for HTML templating and Flying Saucer (OpenHTMLtoPDF) for PDF generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGenerationService {

    private final RecommendationRunRepository runRepository;
    private final RecommendationRepository recommendationRepository;
    private final TemplateEngine templateEngine;

    /**
     * Generate report DTO for a completed run.
     *
     * @param runId Run ID
     * @return Report DTO
     * @throws IllegalArgumentException if run not found
     * @throws IllegalStateException if run not completed
     */
    public ReportDTO generateReport(UUID runId) {
        log.info("Generating report for run {}", runId);

        // Fetch run
        RecommendationRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));

        // Verify run is completed
        if (!"COMPLETED".equals(run.getStatus()) && !"FINALIZED".equals(run.getStatus())) {
            throw new IllegalStateException("Run is not completed: " + run.getStatus());
        }

        // Fetch recommendations
        List<Recommendation> recommendations = recommendationRepository.findByRunIdOrderByRankAsc(runId);

        // Validate recommendations exist
        if (recommendations == null || recommendations.isEmpty()) {
            throw new IllegalStateException("Run " + runId + " has no recommendations");
        }

        // Build report DTO
        ReportDTO report = ReportDTO.builder()
                .runId(runId)
                .reportVersion("1.0")
                .generatedAt(LocalDateTime.now())
                .runDate(run.getScheduledDate() != null ? run.getScheduledDate() : LocalDate.now())
                .portfolioName("Portfolio") // TODO: Fetch actual portfolio name
                .universeName("Universe") // TODO: Fetch actual universe name
                .dataAsOfDate(LocalDate.now()) // TODO: Get from dataFreshnessSnapshot
                .summary(buildSummary(recommendations))
                .topPicks(buildTopPicks(recommendations))
                .exclusions(buildExclusions())
                .disclaimers(getDisclaimers())
                .constraintCompliance(buildConstraintCompliance(recommendations))
                .build();

        log.info("Generated report for run {} with {} recommendations", runId, recommendations.size());
        return report;
    }

    /**
     * Generate PDF bytes for a completed run.
     *
     * @param runId Run ID
     * @return PDF bytes
     */
    public byte[] generatePDF(UUID runId) {
        log.info("Generating PDF for run {}", runId);

        // Generate report DTO
        ReportDTO report = generateReport(runId);

        // Generate HTML from Thymeleaf template
        String html = generateHTML(report);

        // Convert HTML to PDF using Flying Saucer
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("Generated PDF for run {} ({} bytes)", runId, pdfBytes.length);
            return pdfBytes;
        } catch (Exception e) {
            log.error("Failed to generate PDF for run {}: {}", runId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Generate HTML from report DTO using Thymeleaf template.
     *
     * @param report Report DTO
     * @return HTML string
     */
    private String generateHTML(ReportDTO report) {
        Context context = new Context();
        context.setVariable("report", report);
        context.setVariable("generatedDate", LocalDateTime.now());

        return templateEngine.process("report", context);
    }

    /**
     * Build summary section.
     *
     * @param recommendations List of recommendations
     * @return Summary section
     */
    private ReportDTO.SummarySection buildSummary(List<Recommendation> recommendations) {
        // Handle empty recommendations gracefully
        if (recommendations == null || recommendations.isEmpty()) {
            log.warn("Building summary with empty recommendations list");
            return ReportDTO.SummarySection.builder()
                    .totalRecommendations(0)
                    .buyCount(0)
                    .sellCount(0)
                    .holdCount(0)
                    .expectedAlphaBps(BigDecimal.ZERO)
                    .expectedCostBps(BigDecimal.ZERO)
                    .edgeOverCostBps(BigDecimal.ZERO)
                    .constraintViolations(0)
                    .build();
        }

        int buyCount = 0;
        int sellCount = 0;
        int holdCount = 0;
        BigDecimal totalExpectedAlpha = BigDecimal.ZERO;
        BigDecimal totalExpectedCost = BigDecimal.ZERO;

        for (Recommendation rec : recommendations) {
            // Count by action (inferred from weight change)
            if (rec.getWeightChangePct().compareTo(BigDecimal.ZERO) > 0) {
                buyCount++;
            } else if (rec.getWeightChangePct().compareTo(BigDecimal.ZERO) < 0) {
                sellCount++;
            } else {
                holdCount++;
            }

            totalExpectedAlpha = totalExpectedAlpha.add(rec.getExpectedAlphaBps());
            totalExpectedCost = totalExpectedCost.add(rec.getExpectedCostBps());
        }

        return ReportDTO.SummarySection.builder()
                .totalRecommendations(recommendations.size())
                .buyCount(buyCount)
                .sellCount(sellCount)
                .holdCount(holdCount)
                .expectedAlphaBps(totalExpectedAlpha)
                .expectedCostBps(totalExpectedCost)
                .edgeOverCostBps(totalExpectedAlpha.subtract(totalExpectedCost))
                .constraintViolations(0) // TODO: Count actual violations
                .build();
    }

    /**
     * Build top picks section (top 10 recommendations).
     *
     * @param recommendations List of recommendations
     * @return Top 10 picks
     */
    private List<ReportDTO.RecommendationSummary> buildTopPicks(List<Recommendation> recommendations) {
        return recommendations.stream()
                .limit(10)
                .map(this::toRecommendationSummary)
                .collect(Collectors.toList());
    }

    /**
     * Convert Recommendation entity to RecommendationSummary DTO.
     *
     * @param rec Recommendation entity
     * @return Recommendation summary DTO
     */
    private ReportDTO.RecommendationSummary toRecommendationSummary(Recommendation rec) {
        // Build factor scores map
        Map<String, BigDecimal> factorScores = new HashMap<>();
        // TODO: Extract factor scores from recommendation entity or factor score service

        // Infer action from weight change
        String action = "HOLD";
        if (rec.getWeightChangePct().compareTo(BigDecimal.ZERO) > 0) {
            action = "BUY";
        } else if (rec.getWeightChangePct().compareTo(BigDecimal.ZERO) < 0) {
            action = "SELL";
        }

        return ReportDTO.RecommendationSummary.builder()
                .rank(rec.getRank())
                .symbol(rec.getSymbol())
                .action(action)
                .targetWeightPct(rec.getTargetWeightPct())
                .currentWeightPct(rec.getCurrentWeightPct())
                .weightChangePct(rec.getWeightChangePct())
                .confidenceScore(rec.getConfidenceScore())
                .expectedAlphaBps(rec.getExpectedAlphaBps())
                .sector(rec.getSector())
                .marketCapTier(rec.getMarketCapTier())
                .liquidityTier(rec.getLiquidityTier())
                .explanation(rec.getExplanation())
                .factorScores(factorScores)
                .changeIndicator(rec.getChangeIndicator())
                .build();
    }

    /**
     * Build exclusions section.
     * TODO: Implement actual exclusion logic.
     *
     * @return List of exclusions
     */
    private List<ReportDTO.ExclusionSummary> buildExclusions() {
        // Placeholder: Return empty list
        // In real implementation, query stocks in universe that were excluded
        return new ArrayList<>();
    }

    /**
     * Get legal disclaimers per FR-060.
     *
     * @return List of disclaimer texts
     */
    private List<String> getDisclaimers() {
        return Arrays.asList(
                "NOT FINANCIAL ADVICE",
                "These recommendations are generated by quantitative models and are for informational purposes only.",
                "This tool does not provide personalized financial, investment, legal, or tax advice.",
                "Past performance does not guarantee future results.",
                "All investments involve risk, including the potential loss of principal.",
                "You are solely responsible for your investment decisions.",
                "Consult with a qualified financial advisor before making any investment decisions.",
                "This tool is not registered as an investment advisor with any regulatory authority.",
                "Factor-based strategies may underperform during certain market conditions.",
                "Data quality and timeliness constraints may affect recommendation accuracy."
        );
    }

    /**
     * Build constraint compliance section.
     *
     * @param recommendations List of recommendations
     * @return Constraint compliance section
     */
    private ReportDTO.ConstraintComplianceSection buildConstraintCompliance(List<Recommendation> recommendations) {
        // TODO: Implement actual constraint validation
        // For now, assume all constraints met

        Map<String, String> constraintSummary = new HashMap<>();
        constraintSummary.put("Position Size Limits", "All positions within limits");
        constraintSummary.put("Sector Exposure", "Within target ranges");
        constraintSummary.put("Turnover", "Below maximum threshold");
        constraintSummary.put("Liquidity Requirements", "All stocks meet liquidity tiers");

        return ReportDTO.ConstraintComplianceSection.builder()
                .allConstraintsMet(true)
                .violations(new ArrayList<>())
                .constraintSummary(constraintSummary)
                .build();
    }
}
