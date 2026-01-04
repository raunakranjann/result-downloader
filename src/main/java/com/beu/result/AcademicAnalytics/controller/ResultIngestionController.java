package com.beu.result.AcademicAnalytics.controller;

import com.beu.result.AcademicAnalytics.config.ResultSourceConfig;
import com.beu.result.AcademicAnalytics.service.TranscriptGenerationService;
import com.beu.result.AcademicAnalytics.service.DataSyncStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the Data Ingestion Module.
 * <p>
 * This controller manages the administrative interface for triggering data synchronization jobs.
 * It exposes REST endpoints for the frontend JavaScript queue manager to initiate batch processing
 * and poll for progress updates.
 * </p>
 */
@Controller
public class ResultIngestionController {

    private final TranscriptGenerationService ingestionService;
    private final ResultSourceConfig sourceConfig;
    private final DataSyncStatus syncStatus;

    // Dependency Injection
    public ResultIngestionController(TranscriptGenerationService ingestionService,
                                     ResultSourceConfig sourceConfig,
                                     DataSyncStatus syncStatus) {
        this.ingestionService = ingestionService;
        this.sourceConfig = sourceConfig;
        this.syncStatus = syncStatus;
    }

    // ==========================================
    // VIEW CONTROLLER
    // ==========================================

    /**
     * Renders the Ingestion Dashboard UI.
     * Accessible via /admin/ingestion-portal
     */
    @GetMapping("/admin/ingestion-portal")
    public String showIngestionDashboard(Model model) {
        // Inject the registry of available data sources into the view
        model.addAttribute("linkMap", sourceConfig.getAllLinks());

        // Return the template name (Rename your HTML file to 'ingestion-dashboard.html')
        return "ingestion-dashboard";
    }

    // ==========================================
    // REST API (AJAX/Queue Handlers)
    // ==========================================

    /**
     * API Endpoint: Initiate Batch Ingestion.
     * <p>
     * Called asynchronously by the frontend job queue. Triggers the service layer
     * to fetch and parse records for the specified range.
     * </p>
     *
     * @param linkKey  The identifier for the data source (e.g., "5th Sem 2025").
     * @param startReg The starting Registration Number.
     * @param endReg   The ending Registration Number.
     * @return JSON response indicating job initiation status.
     */
    @PostMapping("/api/ingestion/start-batch")
    @ResponseBody
    public Map<String, String> initiateIngestionBatch(
            @RequestParam String linkKey,
            @RequestParam long startReg,
            @RequestParam long endReg
    ) {
        // Resolve the dynamic URL pattern
        String urlPattern = sourceConfig.getUrl(linkKey);

        // Fallback safety: if key is not found, assume raw URL was passed (validation logic)
        if (urlPattern == null) {
            urlPattern = linkKey;
        }

        // Delegate execution to the Business Logic Layer
        ingestionService.processResultRange(urlPattern, startReg, endReg);

        // Construct standardized JSON success response
        Map<String, String> response = new HashMap<>();
        response.put("status", "BATCH_INITIATED");
        response.put("message", "Ingestion job queued successfully.");

        return response;
    }

    /**
     * API Endpoint: Poll Progress Telemetry.
     * <p>
     * Used by the frontend progress bar to display real-time status.
     * </p>
     *
     * @return Current state object containing processed count and operational logs.
     */
    @GetMapping("/api/ingestion/progress")
    @ResponseBody
    public DataSyncStatus getIngestionTelemetry() {
        return syncStatus;
    }
}