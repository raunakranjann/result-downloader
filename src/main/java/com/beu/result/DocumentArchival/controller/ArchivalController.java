package com.beu.result.DocumentArchival.controller;

import com.beu.result.DocumentArchival.config.ArchivalJobRequest;
import com.beu.result.DocumentArchival.service.CertificateGenerationService;
import com.beu.result.DocumentArchival.util.ArchivalTelemetry;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import java.util.stream.Stream;

/**
 * Controller for the Digital Document Archive Module.
 * <p>
 * Manages the "Archival Console" UI, triggers asynchronous PDF generation jobs,
 * and handles the secure streaming of generated ZIP/PDF artifacts.
 * </p>
 */
@Controller
@RequestMapping("/admin/archives")
public class ArchivalController {

    private static final Logger LOG = LoggerFactory.getLogger(ArchivalController.class);

    private final CertificateGenerationService archivalService;
    private final ArchivalTelemetry telemetry;

    public ArchivalController(CertificateGenerationService archivalService, ArchivalTelemetry telemetry) {
        this.archivalService = archivalService;
        this.telemetry = telemetry;
    }

    /**
     * Resolves the standard storage location for generated artifacts.
     * Default: UserHome/Documents/Academic_Archives
     */
    private Path getStorageDirectory() {
        return Paths.get(System.getProperty("user.home"), "Documents", "Academic_Archives");
    }

    // ==========================================
    // 1. CONSOLE UI
    // ==========================================

    /**
     * Renders the Archival Management Console.
     * URL: /admin/archives/console
     */
    @GetMapping("/console")
    public String showArchivalConsole(Model model) {
        ArchivalJobRequest defaultRequest = new ArchivalJobRequest();

        // Set Smart Defaults
        defaultRequest.setTargetSemester("V");
        defaultRequest.setAcademicSession(2024);
        defaultRequest.setPublicationCycle("July/2025");
        defaultRequest.setStorageLocation(getStorageDirectory().toAbsolutePath().toString());

        model.addAttribute("jobRequest", defaultRequest);
        return "archival-console"; // Maps to templates/archival-console.html
    }

    // ==========================================
    // 2. JOB EXECUTION API
    // ==========================================

    /**
     * Initiates an Asynchronous Archival Batch Job.
     * URL: /admin/archives/api/initiate
     */
    @PostMapping("/api/initiate")
    @ResponseBody
    public ResponseEntity<Map<String, String>> initiateArchivalJob(@ModelAttribute ArchivalJobRequest jobRequest) {

        // 1. Validate Storage Directory
        File storageDir = new File(jobRequest.getStorageLocation());
        if (!storageDir.exists()) {
            boolean created = storageDir.mkdirs();
            if(!created) LOG.warn("Could not create directory, checking permissions.");
        }

        // 2. Validate Input Range
        if (jobRequest.getRangeStart() == null || jobRequest.getRangeEnd() == null ||
                jobRequest.getRangeStart() > jobRequest.getRangeEnd()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid Registration Number Range"));
        }

        // 3. Trigger Async Service (Non-Blocking)
        archivalService.generateCertificates(jobRequest);

        // 4. Return Immediate Success Response
        Map<String, String> response = new HashMap<>();
        response.put("status", "JOB_INITIATED");
        response.put("message", "Archival process running in background.");

        return ResponseEntity.ok(response);
    }

    /**
     * Polls the current status of the active job.
     * URL: /admin/archives/api/telemetry
     */
    @GetMapping("/api/telemetry")
    @ResponseBody
    public ArchivalTelemetry getJobTelemetry() {
        return telemetry;
    }

    // ==========================================
    // 3. DOWNLOAD HANDLERS
    // ==========================================

    /**
     * Streams a ZIP archive of all individual PDF transcripts.
     * URL: /admin/archives/download/zip
     */
    @GetMapping("/download/zip")
    public void downloadBatchZip(@RequestParam("year") int year,
                                 @RequestParam("semester") String semester,
                                 HttpServletResponse response) {

        Path sourceDir = getStorageDirectory();
        String zipFilename = "Academic_Records_" + semester + "_Sem_" + year + ".zip";
        String mergedFileToExclude = "Merged_Transcript_" + semester + "_Sem_" + year + ".pdf";

        if (!Files.exists(sourceDir)) {
            throw new RuntimeException("Archive repository not found at: " + sourceDir);
        }

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFilename + "\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream());
             Stream<Path> paths = Files.list(sourceDir)) {

            paths.filter(path -> !Files.isDirectory(path))
                    .filter(path -> path.toString().toLowerCase().endsWith(".pdf"))
                    .filter(path -> !path.getFileName().toString().equalsIgnoreCase(mergedFileToExclude))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(path.getFileName().toString());
                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            LOG.error("Failed to compress file: " + path, e);
                        }
                    });

            LOG.info("ZIP Archive downloaded: {}", zipFilename);

        } catch (IOException e) {
            LOG.error("ZIP Generation failed", e);
            throw new RuntimeException("Failed to generate archive bundle", e);
        }
    }

    /**
     * Streams the single Merged PDF document.
     * URL: /admin/archives/download/merged
     */
    @GetMapping("/download/merged")
    public ResponseEntity<FileSystemResource> downloadMergedPdf(@RequestParam("year") int year,
                                                                @RequestParam("semester") String semester) {

        Path sourceDir = getStorageDirectory();
        String filename = "Merged_Transcript_" + semester + "_Sem_" + year + ".pdf";
        Path file = sourceDir.resolve(filename);

        if (!Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }

        LOG.info("Merged PDF downloaded: {}", filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new FileSystemResource(file));
    }
}