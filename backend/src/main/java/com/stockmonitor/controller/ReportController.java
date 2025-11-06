package com.stockmonitor.controller;

import com.stockmonitor.dto.ReportDTO;
import com.stockmonitor.service.ReportGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Controller for report generation per FR-041, FR-042, FR-043, FR-044.
 *
 * T110: REST endpoints for report access and download.
 *
 * Endpoints:
 * - GET /api/runs/{id}/report - Get report DTO as JSON
 * - GET /api/runs/{id}/report/pdf - Download PDF report
 */
@RestController
@RequestMapping("/api/runs")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportGenerationService reportGenerationService;

    /**
     * Get report for a completed run.
     *
     * @param id Run ID
     * @return Report DTO
     */
    @GetMapping("/{id}/report")
    @PreAuthorize("hasRole('OWNER') or hasRole('VIEWER')")
    public ResponseEntity<ReportDTO> getReport(@PathVariable UUID id) {
        log.info("GET /api/runs/{}/report - Fetching report", id);

        try {
            ReportDTO report = reportGenerationService.generateReport(id);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            log.error("Run not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.error("Run not completed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to generate report for run {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Download PDF report for a completed run.
     *
     * @param id Run ID
     * @return PDF bytes with appropriate headers
     */
    @GetMapping("/{id}/report/pdf")
    @PreAuthorize("hasRole('OWNER') or hasRole('VIEWER')")
    public ResponseEntity<byte[]> downloadReportPDF(@PathVariable UUID id) {
        log.info("GET /api/runs/{}/report/pdf - Downloading PDF", id);

        try {
            byte[] pdfBytes = reportGenerationService.generatePDF(id);

            // Generate filename with timestamp
            String filename = String.format(
                    "recommendation-report-%s-%s.pdf",
                    id.toString().substring(0, 8),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (IllegalArgumentException e) {
            log.error("Run not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.error("Run not completed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to generate PDF for run {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
