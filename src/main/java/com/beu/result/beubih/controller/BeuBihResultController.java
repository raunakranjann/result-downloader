package com.beu.result.beubih.controller;

import com.beu.result.beubih.config.BeuBihDownloaderConfig;
import com.beu.result.beubih.service.BeuBihResultPrintService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
@RequestMapping("/beu-bih")
public class BeuBihResultController {

    private final BeuBihResultPrintService service;

    public BeuBihResultController(BeuBihResultPrintService service) {
        this.service = service;
    }

    private Path getFixedDownloadPath() {
        return Paths.get(System.getProperty("user.home"), "Downloads", "Results");
    }

    @GetMapping
    public String showForm(Model model) {
        BeuBihDownloaderConfig cfg = new BeuBihDownloaderConfig();
        cfg.setSemester("I");
        cfg.setExamYear(2024);
        cfg.setExamHeld("July/2025");
        cfg.setOutputDir(getFixedDownloadPath().toAbsolutePath().toString());
        model.addAttribute("config", cfg);
        return "beubih-form";
    }

    @PostMapping("/print")
    @ResponseBody
    public ResponseEntity<?> printResults(@ModelAttribute BeuBihDownloaderConfig cfg) {
        String fixedPath = getFixedDownloadPath().toAbsolutePath().toString();
        cfg.setOutputDir(fixedPath);
        File dir = new File(fixedPath);
        if (!dir.exists()) dir.mkdirs();

        if (cfg.getStartReg() <= 0 || cfg.getEndReg() <= 0 || cfg.getStartReg() > cfg.getEndReg()) {
            return ResponseEntity.badRequest().body("Invalid registration range");
        }

        new Thread(() -> service.printAll(cfg)).start();

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Generating files...");
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

    // ==========================================
    // 1. DOWNLOAD ZIP (Excludes Merged PDF)
    // ==========================================
    @GetMapping("/download-zip")
    public void downloadZip(@RequestParam("year") int year,
                            @RequestParam("semester") String semester,
                            HttpServletResponse response) {

        Path sourceDir = getFixedDownloadPath();
        String zipFilename = "Results_" + year + "_" + semester + ".zip";

        // NAME MUST MATCH SERVICE EXACTLY
        String mergedFileNameToExclude = "Merged_Result_" + semester + "_Sem_" + year + ".pdf";

        if (!Files.exists(sourceDir)) throw new RuntimeException("Results folder not found!");

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFilename + "\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            Files.list(sourceDir)
                    .filter(path -> !Files.isDirectory(path))
                    .filter(path -> path.toString().toLowerCase().endsWith(".pdf"))
                    // FILTER: Exclude the merged file
                    .filter(path -> !path.getFileName().toString().equalsIgnoreCase(mergedFileNameToExclude))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(path.getFileName().toString());
                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) { e.printStackTrace(); }
                    });
        } catch (IOException e) { throw new RuntimeException("Error generating zip", e); }
    }

    // ==========================================
    // 2. DOWNLOAD MERGED PDF
    // ==========================================
    @GetMapping("/download-merged")
    public void downloadMergedPdf(@RequestParam("year") int year,
                                  @RequestParam("semester") String semester,
                                  HttpServletResponse response) {

        Path sourceDir = getFixedDownloadPath();
        // NAME MUST MATCH SERVICE EXACTLY
        String filename = "Merged_Result_" + semester + "_Sem_" + year + ".pdf";
        Path file = sourceDir.resolve(filename);

        if (!Files.exists(file)) throw new RuntimeException("Merged PDF not found! Looking for: " + filename);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try {
            Files.copy(file, response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) { throw new RuntimeException("Error downloading PDF", e); }
    }
}