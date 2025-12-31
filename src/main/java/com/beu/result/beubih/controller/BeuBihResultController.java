package com.beu.result.beubih.controller;

import com.beu.result.beubih.config.BeuBihDownloaderConfig;
import com.beu.result.beubih.service.BeuBihResultPrintService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/beu-bih")
public class BeuBihResultController {

    private final BeuBihResultPrintService service;

    public BeuBihResultController(BeuBihResultPrintService service) {
        this.service = service;
    }

    /**
     * Show BEU-BIH Result Printer Form
     */
    @GetMapping
    public String showForm(Model model) {

        // IMPORTANT: fresh config object per request
        BeuBihDownloaderConfig cfg = new BeuBihDownloaderConfig();

        // sensible defaults (can be changed from UI)
        cfg.setSemester("I");                // I, II, III, IV, V, VI, VII, VIII
        cfg.setExamYear(2024);               // Examination year
        cfg.setExamHeld("July/2025");        // Month/Year
        cfg.setOutputDir("D:/beu-bih-results");

        model.addAttribute("config", cfg);
        return "beubih-form";
    }

    /**
     * Trigger bulk printing of results
     */
    @PostMapping("/print")
    public String printResults(
            @ModelAttribute("config") BeuBihDownloaderConfig cfg,
            Model model
    ) {
        System.out.println("===== BEU-BIH PRINT REQUEST RECEIVED =====");
        System.out.println("Semester     : " + cfg.getSemester());
        System.out.println("Exam Year    : " + cfg.getExamYear());
        System.out.println("Exam Held    : " + cfg.getExamHeld());
        System.out.println("Reg From     : " + cfg.getStartReg());
        System.out.println("Reg To       : " + cfg.getEndReg());
        System.out.println("Output Dir   : " + cfg.getOutputDir());

        if (cfg.getStartReg() <= 0 || cfg.getEndReg() <= 0
                || cfg.getStartReg() > cfg.getEndReg()) {

            model.addAttribute("error", "Invalid registration number range");
            return "beubih-form";
        }

        // 🔥 IMPORTANT: run printing in a NEW THREAD
        new Thread(() -> service.printAll(cfg)).start();

        model.addAttribute("success",
                "Printing started in background. Check output folder.");

        return "beubih-form";
    }


    @GetMapping("/progress")
    @ResponseBody
    public Object progress() {
        return new Object() {
            public final int total = com.beu.result.beubih.util.PrintProgress.getTotal();
            public final int completed = com.beu.result.beubih.util.PrintProgress.getCompleted();
            public final boolean running = com.beu.result.beubih.util.PrintProgress.isRunning();
        };
    }

}
