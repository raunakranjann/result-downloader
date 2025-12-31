package com.beu.result.beubih.controller;

import com.beu.result.beubih.config.BeuBihDownloaderConfig;
import com.beu.result.beubih.service.BeuBihResultPrintService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/beu-bih")
public class BeuBihResultController {

    private final BeuBihResultPrintService service;

    public BeuBihResultController(BeuBihResultPrintService service) {
        this.service = service;
    }

    /**
     * Show Form with "Downloads/Results" folder pre-selected
     */
    @GetMapping
    public String showForm(Model model) {
        BeuBihDownloaderConfig cfg = new BeuBihDownloaderConfig();

        // 1. Get User Home (e.g., C:\Users\Raunak or /home/raunak)
        String userHome = System.getProperty("user.home");

        // 2. Build path: Home -> Downloads -> Results
        // This works for both Windows (\) and Linux (/) automatically
        String resultPath = Paths.get(userHome, "Downloads", "Results").toAbsolutePath().toString();

        // 3. Set Defaults
        cfg.setSemester("I");
        cfg.setExamYear(2024);
        cfg.setExamHeld("July/2025");

        // 4. Set the Output Directory
        cfg.setOutputDir(resultPath);

        model.addAttribute("config", cfg);
        return "beubih-form";
    }

    /**
     * Handle Print Request
     */
    @PostMapping("/print")
    @ResponseBody
    public ResponseEntity<?> printResults(@ModelAttribute BeuBihDownloaderConfig cfg) {

        // Fallback: If for some reason the path is empty, reset it to Downloads/Results
        if (cfg.getOutputDir() == null || cfg.getOutputDir().trim().isEmpty()) {
            String userHome = System.getProperty("user.home");
            cfg.setOutputDir(Paths.get(userHome, "Downloads", "Results").toString());
        }

        System.out.println("===== PRINT REQUEST =====");
        System.out.println("Target Folder: " + cfg.getOutputDir());

        // 5. AUTO-CREATE FOLDER: If "Results" folder doesn't exist, create it
        File dir = new File(cfg.getOutputDir());
        if (!dir.exists()) {
            boolean created = dir.mkdirs(); // mkdirs() creates parent folders if needed
            if(created) System.out.println("Created directory: " + cfg.getOutputDir());
        }

        // Validate Inputs
        if (cfg.getStartReg() <= 0 || cfg.getEndReg() <= 0 || cfg.getStartReg() > cfg.getEndReg()) {
            return ResponseEntity.badRequest().body("Invalid registration range");
        }

        // Run printing in background thread
        new Thread(() -> service.printAll(cfg)).start();

        // Return JSON success
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Downloading to: " + cfg.getOutputDir());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/progress")
    @ResponseBody
    public Map<String, Object> progress() {
        Map<String, Object> status = new HashMap<>();
        status.put("total", com.beu.result.beubih.util.PrintProgress.getTotal());
        status.put("completed", com.beu.result.beubih.util.PrintProgress.getCompleted());
        status.put("running", com.beu.result.beubih.util.PrintProgress.isRunning());
        return status;
    }
}