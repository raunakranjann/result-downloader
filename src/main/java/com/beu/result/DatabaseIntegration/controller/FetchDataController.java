package com.beu.result.DatabaseIntegration.controller;

import com.beu.result.DatabaseIntegration.config.ResultLinkConfig;
import com.beu.result.DatabaseIntegration.service.ResultPrintService;
import com.beu.result.DatabaseIntegration.service.ScrapingState;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller responsible for managing the data scraping workflow.
 * It provides endpoints to render the control panel, trigger the scraping process
 * asynchronously, and poll the server for real-time progress updates.
 */
@Controller
public class FetchDataController {

    private final ResultPrintService service;
    private final ResultLinkConfig linkConfig;
    private final ScrapingState state;

    public FetchDataController(ResultPrintService service, ResultLinkConfig linkConfig, ScrapingState state) {
        this.service = service;
        this.linkConfig = linkConfig;
        this.state = state;
    }

    /**
     * Renders the data fetching control panel view.
     * Pre-loads the available result link configurations into the model.
     */
    @GetMapping("/fetch-data")
    public String home(Model model) {
        model.addAttribute("linkMap", linkConfig.getAllLinks());
        return "fetch-data";
    }

    /**
     * Initiates the result scraping process for a specified range of registration numbers.
     * This method triggers the service asynchronously and immediately returns a JSON status,
     * allowing the client to start polling for progress without blocking.
     */
    @PostMapping("/process")
    @ResponseBody
    public Map<String, String> startProcess(
            @RequestParam String linkKey,
            @RequestParam long startReg,
            @RequestParam long endReg
    ) {
        String urlPattern = linkConfig.getUrl(linkKey);
        service.processResultRange(urlPattern, startReg, endReg);

        Map<String, String> response = new HashMap<>();
        response.put("status", "started");
        return response;
    }

    /**
     * Provides the current state of the scraping operation.
     * Designed to be polled by the frontend JavaScript to update the progress bar.
     */
    @GetMapping("/api/progress")
    @ResponseBody
    public ScrapingState getProgress() {
        return state;
    }
}