package com.stockmonitor.service;

import com.stockmonitor.dto.ExclusionDTO;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for exporting exclusions to CSV (T196, FR-032).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExclusionExportService {

  private final ExclusionReasonService exclusionReasonService;

  public String exportToCsv(UUID runId) {
    List<ExclusionDTO> exclusions = exclusionReasonService.getExclusionsForRun(runId);

    StringWriter writer = new StringWriter();
    writer.append("symbol,company_name,exclusion_reason_code,explanation,run_date\r\n");

    for (ExclusionDTO exclusion : exclusions) {
      writer.append(quoteCsvField(exclusion.getSymbol())).append(",");
      writer.append(quoteCsvField(exclusion.getCompanyName())).append(",");
      writer.append(quoteCsvField(exclusion.getExclusionReasonCode())).append(",");
      writer.append(quoteCsvField(exclusion.getExplanation())).append(",");
      writer.append(quoteCsvField(exclusion.getRunDate().toString())).append("\r\n");
    }

    return writer.toString();
  }

  private String quoteCsvField(String value) {
    if (value == null) return "\"\"";
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }
}
