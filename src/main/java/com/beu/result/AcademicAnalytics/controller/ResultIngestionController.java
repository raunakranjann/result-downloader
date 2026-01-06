package com.beu.result.AcademicAnalytics.controller;

import com.beu.result.AcademicAnalytics.config.ResultSourceConfig;
import com.beu.result.AcademicAnalytics.service.DataSyncStatus;
import com.beu.result.AcademicAnalytics.service.TranscriptGenerationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the Data Ingestion Module.
 * Manages the UI and API for triggering academic record synchronization.
 */
@Controller
public class ResultIngestionController {

    private final TranscriptGenerationService ingestionService;
    private final ResultSourceConfig sourceConfig;
    private final DataSyncStatus syncStatus;

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
     * Endpoint: /admin/ingestion-portal
     */
    @GetMapping("/admin/ingestion-portal")
    public String showIngestionDashboard(Model model) {
        // Fetches available links from DB (via ResultSourceConfig) for the dropdown
        model.addAttribute("linkMap", sourceConfig.getAllLinks());
        return "ingestion-dashboard";
    }

    // ==========================================
    // REST API (AJAX/Queue Handlers)
    // ==========================================

    /**
     * API Endpoint: Initiate Batch Ingestion.
     */
    @PostMapping("/api/ingestion/start-batch")
    @ResponseBody
    public Map<String, Object> initiateIngestionBatch(
            @RequestParam String linkKey,
            @RequestParam long startReg,
            @RequestParam long endReg
    ) {
        Map<String, Object> response = new HashMap<>();

        // Validation
        if (startReg > endReg) {
            response.put("status", "ERROR");
            response.put("message", "Start Registration cannot be greater than End Registration.");
            return response;
        }

        if (syncStatus.isJobActive()) {
            response.put("status", "BUSY");
            response.put("message", "A job is already running. Please wait.");
            return response;
        }

        // Delegate to Service
        // The service now handles both "Keys" (db lookup) and "Raw URLs"
        ingestionService.processResultRange(linkKey, startReg, endReg);

        response.put("status", "BATCH_INITIATED");
        response.put("message", "Ingestion job queued for range: " + startReg + " - " + endReg);
        return response;
    }

    /**
     * API Endpoint: Poll Progress Telemetry.
     */
    @GetMapping("/api/ingestion/progress")
    @ResponseBody
    public DataSyncStatus getIngestionTelemetry() {
        return syncStatus;
    }
}