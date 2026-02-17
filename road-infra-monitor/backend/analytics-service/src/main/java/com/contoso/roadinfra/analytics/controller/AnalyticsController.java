package com.contoso.roadinfra.analytics.controller;

import com.contoso.roadinfra.analytics.entity.KpiSnapshot;
import com.contoso.roadinfra.analytics.service.AnalyticsService;
import com.contoso.roadinfra.analytics.service.KpiCalculationService;
import com.contoso.roadinfra.analytics.service.ReportExportService;
import com.contoso.roadinfra.common.dto.ApiResponse;
import com.contoso.roadinfra.common.dto.KpiDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "KPI and analytics endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final KpiCalculationService kpiCalculationService;
    private final ReportExportService reportExportService;

    // ================= Legacy KPI Endpoints =================

    @GetMapping("/kpis/{id}")
    @Operation(summary = "Get KPI by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<KpiDTO>> getKpiById(@PathVariable UUID id) {
        KpiDTO kpi = analyticsService.getKpiById(id);
        return ResponseEntity.ok(ApiResponse.success(kpi));
    }

    @GetMapping("/kpis/latest/{metricName}")
    @Operation(summary = "Get latest KPI by metric name")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<KpiDTO>> getLatestKpi(@PathVariable String metricName) {
        KpiDTO kpi = analyticsService.getLatestKpi(metricName);
        return ResponseEntity.ok(ApiResponse.success(kpi));
    }

    @GetMapping("/kpis/category/{category}")
    @Operation(summary = "Get KPIs by category")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<KpiDTO>>> getKpisByCategory(@PathVariable String category) {
        List<KpiDTO> kpis = analyticsService.getKpisByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(kpis));
    }

    @GetMapping("/kpis/asset/{assetId}")
    @Operation(summary = "Get KPIs for an asset")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<Page<KpiDTO>>> getKpisByAsset(
            @PathVariable UUID assetId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<KpiDTO> kpis = analyticsService.getKpisByAsset(assetId, pageable);
        return ResponseEntity.ok(ApiResponse.success(kpis));
    }

    @GetMapping("/kpis/off-target")
    @Operation(summary = "Get off-target KPIs")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<KpiDTO>>> getOffTargetKpis() {
        List<KpiDTO> kpis = analyticsService.getOffTargetKpis();
        return ResponseEntity.ok(ApiResponse.success(kpis));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get all metric names")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<String>>> getAllMetricNames() {
        List<String> metrics = analyticsService.getAllMetricNames();
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get all categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<String>>> getAllCategories() {
        List<String> categories = analyticsService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @PostMapping("/kpis/calculate")
    @Operation(summary = "Calculate a KPI")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<ApiResponse<KpiDTO>> calculateKpi(
            @RequestParam String metricName,
            @RequestParam String category,
            @RequestParam(required = false) UUID assetId,
            @RequestParam(required = false) String assetName,
            @RequestParam Double value,
            @RequestParam(required = false) Double targetValue,
            @RequestParam(required = false) String unit,
            @RequestParam(defaultValue = "DAILY") String period) {
        KpiDTO kpi = analyticsService.calculateKpi(metricName, category, assetId, assetName, value, targetValue, unit, period);
        return ResponseEntity.ok(ApiResponse.success(kpi, "KPI calculated"));
    }

    // ================= KPI Snapshot Endpoints =================

    @GetMapping("/snapshots/current")
    @Operation(summary = "Get current KPI snapshots")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<KpiSnapshot>>> getCurrentKpis() {
        List<KpiSnapshot> snapshots = kpiCalculationService.getCurrentKpis();
        return ResponseEntity.ok(ApiResponse.success(snapshots));
    }

    @GetMapping("/snapshots/category/{category}")
    @Operation(summary = "Get KPI snapshots by category")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<KpiSnapshot>>> getSnapshotsByCategory(@PathVariable String category) {
        List<KpiSnapshot> snapshots = kpiCalculationService.getKpisByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(snapshots));
    }

    @GetMapping("/snapshots/history/{metricName}")
    @Operation(summary = "Get KPI history")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<KpiSnapshot>>> getKpiHistory(
            @PathVariable String metricName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<KpiSnapshot> history = kpiCalculationService.getKpiHistory(metricName, from, to);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/dashboard/summary")
    @Operation(summary = "Get dashboard summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardSummary() {
        Map<String, Object> summary = kpiCalculationService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping("/snapshots/recalculate")
    @Operation(summary = "Trigger KPI recalculation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> recalculateKpis() {
        kpiCalculationService.calculateDailyKpis();
        return ResponseEntity.ok(ApiResponse.success(null, "KPI recalculation triggered"));
    }

    // ================= Export Endpoints =================

    @GetMapping("/export/csv")
    @Operation(summary = "Export KPIs to CSV")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<byte[]> exportKpisCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String category) {
        
        LocalDate fromDate = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate toDate = to != null ? to : LocalDate.now();
        
        byte[] csvData = reportExportService.exportKpisCsv(fromDate, toDate, category);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "kpi-report.csv");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }

    @GetMapping("/export/history/{metricName}/csv")
    @Operation(summary = "Export KPI history to CSV")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<byte[]> exportKpiHistoryCsv(
            @PathVariable String metricName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        byte[] csvData = reportExportService.exportKpiHistoryCsv(metricName, from, to);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", metricName + "-history.csv");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }

    @GetMapping("/export/html")
    @Operation(summary = "Export report as HTML")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<String> exportHtmlReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        LocalDate fromDate = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate toDate = to != null ? to : LocalDate.now();
        
        String html = reportExportService.generateHtmlReport(fromDate, toDate);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(html);
    }
}
