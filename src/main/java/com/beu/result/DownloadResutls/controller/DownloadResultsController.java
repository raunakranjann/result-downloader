package com.beu.result.DownloadResutls.controller;

import com.beu.result.DownloadResutls.config.BeuBihDownloaderConfig;
import com.beu.result.DownloadResutls.service.ResultDownloadService;
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

/**
 * Controller responsible for the result downloading module.
 * It handles the UI for selecting exam criteria, triggers the PDF generation process,
 * tracks progress, and provides endpoints to download results as a ZIP archive or a single merged PDF.
 */
@Controller
@RequestMapping("/Download-results")
public class DownloadResultsController {

    private final ResultDownloadService service;

    public DownloadResultsController(ResultDownloadService service) {
        this.service = service;
    }

    /**
     * Helper method to determine the fixed output directory for generated results.
     * Defaults to the 'Downloads/Results' folder in the user's home directory.
     */
    private Path getFixedDownloadPath() {
        return Paths.get(System.getProperty("user.home"), "Downloads", "Results");
    }

    /**
     * Renders the main form for the result downloader.
     * Initializes the configuration object with default values (e.g., Semester V, Year 2024).
     */
    @GetMapping
    public String showForm(Model model) {
        BeuBihDownloaderConfig cfg = new BeuBihDownloaderConfig();
        cfg.setSemester("V");
        cfg.setExamYear(2024);
        cfg.setExamHeld("July/2025");
        cfg.setOutputDir(getFixedDownloadPath().toAbsolutePath().toString());
        model.addAttribute("config", cfg);
        return "Download-results";
    }

    /**
     * Handles the form submission to start the printing process.
     * Validates the input range, ensures the output directory exists, and triggers
     * the scraping service in a separate background thread to avoid blocking the UI.
     */
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

    /**
     * API endpoint to poll the current status of the background printing task.
     * Returns the total records, number completed, and running status.
     */
    @GetMapping("/progress")
    @ResponseBody
    public Map<String, Object> progress() {
        Map<String, Object> status = new HashMap<>();
        status.put("total", com.beu.result.DownloadResutls.util.PrintProgress.getTotal());
        status.put("completed", com.beu.result.DownloadResutls.util.PrintProgress.getCompleted());
        status.put("running", com.beu.result.DownloadResutls.util.PrintProgress.isRunning());
        return status;
    }

    /**
     * Generates and downloads a ZIP archive containing individual student result PDFs.
     * Explicitly excludes the "Merged Result" PDF to prevent duplication within the archive.
     */
    @GetMapping("/download-zip")
    public void downloadZip(@RequestParam("year") int year,
                            @RequestParam("semester") String semester,
                            HttpServletResponse response) {

        Path sourceDir = getFixedDownloadPath();
        String zipFilename = "Results_" + year + "_" + semester + ".zip";
        String mergedFileNameToExclude = "Merged_Result_" + semester + "_Sem_" + year + ".pdf";

        if (!Files.exists(sourceDir)) throw new RuntimeException("Results folder not found!");

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFilename + "\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            Files.list(sourceDir)
                    .filter(path -> !Files.isDirectory(path))
                    .filter(path -> path.toString().toLowerCase().endsWith(".pdf"))
                    .filter(path -> !path.getFileName().toString().equalsIgnoreCase(mergedFileNameToExclude))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(path.getFileName().toString());
                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Error generating zip", e);
        }
    }

    /**
     * Downloads the single merged PDF containing all result pages.
     * Validates the existence of the specific merged file before streaming it to the response.
     */
    @GetMapping("/download-merged")
    public void downloadMergedPdf(@RequestParam("year") int year,
                                  @RequestParam("semester") String semester,
                                  HttpServletResponse response) {

        Path sourceDir = getFixedDownloadPath();
        String filename = "Merged_Result_" + semester + "_Sem_" + year + ".pdf";
        Path file = sourceDir.resolve(filename);

        if (!Files.exists(file)) throw new RuntimeException("Merged PDF not found! Looking for: " + filename);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try {
            Files.copy(file, response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new RuntimeException("Error downloading PDF", e);
        }
    }
}