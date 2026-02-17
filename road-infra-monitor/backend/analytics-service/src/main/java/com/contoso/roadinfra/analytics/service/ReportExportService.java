package com.contoso.roadinfra.analytics.service;

import com.contoso.roadinfra.analytics.constants.Trend;
import com.contoso.roadinfra.analytics.entity.KpiSnapshot;
import com.contoso.roadinfra.analytics.repository.KpiSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting reports in various formats.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExportService {

    private final KpiSnapshotRepository snapshotRepository;
    private final KpiCalculationService kpiCalculationService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Export KPI data to CSV format.
     */
    public byte[] exportKpisCsv(LocalDate from, LocalDate to, String category) {
        log.info("Exporting KPIs to CSV: from={}, to={}, category={}", from, to, category);

        List<KpiSnapshot> snapshots;
        if (category != null && !category.isEmpty()) {
            snapshots = snapshotRepository.findLatestByCategory(category);
        } else {
            snapshots = snapshotRepository.findLatestForAllMetrics();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);

        // CSV Header
        writer.println("Metric Name,Display Name,Category,Date,Value,Previous Value," +
                "Target,Unit,Change %,Trend,On Target");

        // Data rows
        for (KpiSnapshot snapshot : snapshots) {
            writer.printf("%s,%s,%s,%s,%.2f,%s,%.2f,%s,%s,%s,%s%n",
                    escapeCSV(snapshot.getMetricName()),
                    escapeCSV(snapshot.getDisplayName()),
                    escapeCSV(snapshot.getCategory()),
                    snapshot.getSnapshotDate().format(DATE_FORMAT),
                    snapshot.getValue(),
                    snapshot.getPreviousValue() != null ? String.format("%.2f", snapshot.getPreviousValue()) : "",
                    snapshot.getTargetValue(),
                    escapeCSV(snapshot.getUnit()),
                    snapshot.getPercentageChange() != null ? String.format("%.2f", snapshot.getPercentageChange()) : "",
                    snapshot.getTrend() != null ? snapshot.getTrend().getDisplayName() : "",
                    snapshot.getOnTarget() != null ? snapshot.getOnTarget() : ""
            );
        }

        writer.flush();
        return outputStream.toByteArray();
    }

    /**
     * Export KPI history to CSV.
     */
    public byte[] exportKpiHistoryCsv(String metricName, LocalDate from, LocalDate to) {
        log.info("Exporting KPI history to CSV: metric={}, from={}, to={}", metricName, from, to);

        List<KpiSnapshot> history = snapshotRepository.findByMetricNameAndSnapshotDateBetween(
                metricName, from, to);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);

        // CSV Header
        writer.println("Date,Value,Previous Value,Week Ago,Month Ago,Target,Change %,Trend,On Target");

        // Data rows
        for (KpiSnapshot snapshot : history) {
            writer.printf("%s,%.2f,%s,%s,%s,%.2f,%s,%s,%s%n",
                    snapshot.getSnapshotDate().format(DATE_FORMAT),
                    snapshot.getValue(),
                    snapshot.getPreviousValue() != null ? String.format("%.2f", snapshot.getPreviousValue()) : "",
                    snapshot.getWeekAgoValue() != null ? String.format("%.2f", snapshot.getWeekAgoValue()) : "",
                    snapshot.getMonthAgoValue() != null ? String.format("%.2f", snapshot.getMonthAgoValue()) : "",
                    snapshot.getTargetValue(),
                    snapshot.getPercentageChange() != null ? String.format("%.2f", snapshot.getPercentageChange()) : "",
                    snapshot.getTrend() != null ? snapshot.getTrend().getDisplayName() : "",
                    snapshot.getOnTarget() != null ? snapshot.getOnTarget() : ""
            );
        }

        writer.flush();
        return outputStream.toByteArray();
    }

    /**
     * Generate HTML report (can be converted to PDF).
     */
    public String generateHtmlReport(LocalDate from, LocalDate to) {
        log.info("Generating HTML report: from={}, to={}", from, to);

        List<KpiSnapshot> snapshots = snapshotRepository.findLatestForAllMetrics();
        var summary = kpiCalculationService.getDashboardSummary();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html><head>\n");
        html.append("<title>KPI Report - Road Infrastructure Monitoring</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 40px; }\n");
        html.append("h1 { color: #333; }\n");
        html.append("table { border-collapse: collapse; width: 100%; margin-top: 20px; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }\n");
        html.append("th { background-color: #4472C4; color: white; }\n");
        html.append("tr:nth-child(even) { background-color: #f2f2f2; }\n");
        html.append(".on-target { color: green; }\n");
        html.append(".off-target { color: red; }\n");
        html.append(".trend-up { color: green; }\n");
        html.append(".trend-down { color: red; }\n");
        html.append(".summary-box { background: #f8f9fa; padding: 20px; margin: 20px 0; border-radius: 8px; }\n");
        html.append("</style>\n");
        html.append("</head><body>\n");

        // Header
        html.append("<h1>Road Infrastructure Monitoring - KPI Report</h1>\n");
        html.append("<p>Report generated: ").append(LocalDate.now().format(DATE_FORMAT)).append("</p>\n");
        html.append("<p>Period: ").append(from.format(DATE_FORMAT)).append(" to ").append(to.format(DATE_FORMAT)).append("</p>\n");

        // Summary
        html.append("<div class='summary-box'>\n");
        html.append("<h2>Summary</h2>\n");
        html.append("<p><strong>Total KPIs:</strong> ").append(summary.get("totalKpis")).append("</p>\n");
        html.append("<p><strong>On Target:</strong> ").append(summary.get("onTargetCount"))
                .append(" (").append(String.format("%.1f", summary.get("onTargetPercentage"))).append("%)</p>\n");
        html.append("<p><strong>Improving:</strong> ").append(summary.get("improvingCount")).append("</p>\n");
        html.append("<p><strong>Declining:</strong> ").append(summary.get("decliningCount")).append("</p>\n");
        html.append("</div>\n");

        // KPI Table
        html.append("<h2>KPI Details</h2>\n");
        html.append("<table>\n");
        html.append("<tr><th>Metric</th><th>Category</th><th>Value</th><th>Target</th><th>Change</th><th>Trend</th><th>Status</th></tr>\n");

        for (KpiSnapshot kpi : snapshots) {
            html.append("<tr>\n");
            html.append("<td>").append(kpi.getDisplayName()).append("</td>\n");
            html.append("<td>").append(kpi.getCategory()).append("</td>\n");
            html.append("<td>").append(String.format("%.2f %s", kpi.getValue(), kpi.getUnit())).append("</td>\n");
            html.append("<td>").append(String.format("%.2f %s", kpi.getTargetValue(), kpi.getUnit())).append("</td>\n");
            
            String changeClass = kpi.getPercentageChange() != null && kpi.getPercentageChange() > 0 
                    ? "trend-up" : (kpi.getPercentageChange() != null && kpi.getPercentageChange() < 0 ? "trend-down" : "");
            html.append("<td class='").append(changeClass).append("'>")
                    .append(kpi.getPercentageChange() != null ? String.format("%+.2f%%", kpi.getPercentageChange()) : "N/A")
                    .append("</td>\n");
            
            html.append("<td>").append(kpi.getTrend() != null ? kpi.getTrend().getSymbol() + " " + kpi.getTrend().getDisplayName() : "").append("</td>\n");
            
            String statusClass = Boolean.TRUE.equals(kpi.getOnTarget()) ? "on-target" : "off-target";
            html.append("<td class='").append(statusClass).append("'>")
                    .append(Boolean.TRUE.equals(kpi.getOnTarget()) ? "✓ On Target" : "✗ Off Target")
                    .append("</td>\n");
            
            html.append("</tr>\n");
        }

        html.append("</table>\n");
        html.append("</body></html>");

        return html.toString();
    }

    /**
     * Escape CSV special characters.
     */
    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
