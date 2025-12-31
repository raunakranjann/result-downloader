package com.beu.result.beup.controller;

import com.beu.result.beup.service.ResultPrintService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class ResultController {

    private final ResultPrintService service;

    public ResultController(ResultPrintService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String home() {
        return "result-form";
    }

    @PostMapping("/download")
    @ResponseBody
    public String downloadResults(
            @RequestParam String semester,
            @RequestParam String examType,
            @RequestParam int examYear,
            @RequestParam long startReg,
            @RequestParam long endReg,
            @RequestParam String outputDir
    ) {
        service.printAllResults(
                semester,
                examType,
                examYear,
                startReg,
                endReg,
                outputDir
        );

        return "Download started. Check console for progress.";
    }
}
