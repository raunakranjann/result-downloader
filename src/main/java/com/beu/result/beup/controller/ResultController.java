package com.beu.result.beup.controller;

import com.beu.result.beup.config.ResultLinkConfig;
import com.beu.result.beup.service.ResultPrintService;
import com.beu.result.beup.service.ScrapingState;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@Controller
public class ResultController {

    private final ResultPrintService service;
    private final ResultLinkConfig linkConfig;
    private final ScrapingState state;

    public ResultController(ResultPrintService service, ResultLinkConfig linkConfig, ScrapingState state) {
        this.service = service;
        this.linkConfig = linkConfig;
        this.state = state;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("linkMap", linkConfig.getAllLinks());
        return "result-form";
    }

    // 1. Start Process (Returns JSON immediately)
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

    // 2. Poll Progress (Called by JavaScript)
    @GetMapping("/api/progress")
    @ResponseBody
    public ScrapingState getProgress() {
        return state;
    }
}