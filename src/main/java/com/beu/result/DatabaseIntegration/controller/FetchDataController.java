package com.beu.result.DatabaseIntegration.controller;

import com.beu.result.DatabaseIntegration.config.ResultLinkConfig;
import com.beu.result.DatabaseIntegration.service.ResultPrintService;
import com.beu.result.DatabaseIntegration.service.ScrapingState;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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

    // 1. View Page
    @GetMapping("/fetch-data")
    public String home(Model model) {
        model.addAttribute("linkMap", linkConfig.getAllLinks());
        return "fetch-data";
    }

    // 2. API: Start Single Batch (Used by JavaScript Queue)
    @PostMapping("/api/start-batch")
    @ResponseBody // <--- CRITICAL: Returns JSON, not HTML
    public Map<String, String> startBatch(
            @RequestParam String linkKey,
            @RequestParam long startReg,
            @RequestParam long endReg
    ) {
        // Resolve URL (or use key if url not found)
        String urlPattern = linkConfig.getUrl(linkKey);
        if(urlPattern == null) urlPattern = linkKey;

        // Trigger Service
        service.processResultRange(urlPattern, startReg, endReg);

        // Return JSON Success
        Map<String, String> response = new HashMap<>();
        response.put("status", "started");
        return response;
    }

    // 3. API: Check Progress
    @GetMapping("/api/progress")
    @ResponseBody
    public ScrapingState getProgress() {
        return state;
    }
}