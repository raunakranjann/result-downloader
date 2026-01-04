package com.beu.result.DocumentArchival.service;

import com.beu.result.DocumentArchival.config.ArchivalJobRequest;
import com.beu.result.DocumentArchival.util.ArchivalTelemetry;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Service responsible for the automated generation and preservation of academic certificates.
 * <p>
 * Features:
 * 1. Headless Browser Automation (Playwright)
 * 2. Smart Retry Logic for flaky network connections
 * 3. Automatic PDF Merging for batch downloads
 * </p>
 */
@Service
public class CertificateGenerationService {

    private static final Logger LOG = LoggerFactory.getLogger(CertificateGenerationService.class);
    private final ArchivalTelemetry telemetry;

    // Dependency Injection
    public CertificateGenerationService(ArchivalTelemetry telemetry) {
        this.telemetry = telemetry;
    }

    /**
     * Main Entry Point. Executed Asynchronously.
     */
    @Async
    public void generateCertificates(ArchivalJobRequest jobRequest) {

        long startReg = jobRequest.getRangeStart();
        long endReg = jobRequest.getRangeEnd();
        int totalRecords = (int) (endReg - startReg + 1);

        // 1. Initialize Telemetry
        telemetry.initializeJob(totalRecords);
        LOG.info("Starting Archival Batch. Range: {}-{}, Total: {}", startReg, endReg, totalRecords);

        // 2. Prepare Output Directory
        File outputDir = new File(jobRequest.getStorageLocation());
        if (!outputDir.exists()) outputDir.mkdirs();

        // 3. Launch Browser Engine
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));

            // Emulate a Real User Agent to avoid bot detection
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1280, 1024)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));

            Page page = context.newPage();

            // 4. Processing Loop
            for (long currentReg = startReg; currentReg <= endReg; currentReg++) {
                processSingleRecord(page, jobRequest, currentReg, outputDir.getAbsolutePath());
            }

            // 5. Cleanup & Merging
            context.close();
            browser.close();

            LOG.info("Batch Processing Complete. Initiating Merge Sequence...");
            String mergedFileName = "Merged_Transcript_" + jobRequest.getTargetSemester() + "_Sem_" + jobRequest.getAcademicSession() + ".pdf";
            mergePdfArtifacts(jobRequest.getStorageLocation(), mergedFileName);

        } catch (Exception e) {
            LOG.error("Critical Failure in Archival Engine", e);
        } finally {
            telemetry.finalizeJob();
            LOG.info("Archival Job Terminated.");
        }
    }

    /**
     * Handles the logic for a single student record, including retries and validation.
     * UPDATED: Ensures telemetry updates regardless of Success, Failure, or Skip.
     */
    private void processSingleRecord(Page page, ArchivalJobRequest config, long regNo, String outputDir) {
        boolean isArchived = false;
        String logPrefix = "[Record " + regNo + "] ";
        String statusMessage = "Processing " + regNo; // Default status

        // Retry Strategy (Up to 3 attempts)
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                String targetUrl = constructTargetUrl(config, regNo);

                // A. Navigate
                page.navigate(targetUrl, new Page.NavigateOptions()
                        .setTimeout(60000)
                        .setWaitUntil(WaitUntilState.NETWORKIDLE));

                // B. Wait for critical DOM element ("Subject Code" indicates the table is rendering)
                try {
                    page.waitForSelector("text=Subject Code", new Page.WaitForSelectorOptions().setTimeout(5000));
                } catch (TimeoutError ignored) {
                    // Timeout is expected if the page is blank or "No Record Found"
                }

                // C. Validation: Check for "No Record Found"
                if (page.locator("text=No Record Found").count() > 0) {
                    LOG.warn("{} Skipped - Record does not exist.", logPrefix);
                    statusMessage = "Skipped (No Record): " + regNo;
                    isArchived = true; // Treated as handled
                    break;
                }

                // D. Validation: Check Data Table integrity
                Locator resultTable = page.locator("table:has-text('Subject Code')").first();
                if (resultTable.count() > 0) {
                    int rowCount = resultTable.locator("tr").count();

                    if (rowCount > 1) {
                        // VALID DATA FOUND -> PRINT PDF
                        Path pdfPath = Paths.get(outputDir, regNo + ".pdf");
                        page.pdf(new Page.PdfOptions()
                                .setPath(pdfPath)
                                .setFormat("A3")
                                .setPrintBackground(true));

                        LOG.info("{} Archived Successfully.", logPrefix);
                        statusMessage = "Archived: " + regNo + ".pdf";
                        isArchived = true;
                        break;
                    } else {
                        // Table header exists, but no rows -> Empty Data
                        LOG.warn("{} Skipped - Empty Data Table.", logPrefix);
                        statusMessage = "Skipped (Empty Table): " + regNo;
                        isArchived = true;
                        break;
                    }
                } else {
                    // E. No Table Found -> Likely a Network Glitch or White Screen -> RETRY
                    if (attempt < 3) {
                        LOG.debug("{} Content missing. Retrying (Attempt {}/3)...", logPrefix, attempt);
                        Thread.sleep(2000); // Backoff
                    }
                }

            } catch (Exception e) {
                LOG.error("{} Error on attempt {}: {}", logPrefix, attempt, e.getMessage());
            }
        }

        // F. Failure Handling: Screenshot
        if (!isArchived) {
            LOG.error("{} Failed after 3 attempts.", logPrefix);
            captureErrorState(page, outputDir, regNo);
            statusMessage = "Failed: " + regNo;
        }

        // CRITICAL FIX: Update telemetry ONCE per record, regardless of outcome
        telemetry.updateStatus(statusMessage);
    }

    // ==========================================
    // UTILITIES
    // ==========================================

    private String constructTargetUrl(ArchivalJobRequest cfg, long regNo) {
        String examName = "B.Tech. " + resolveSemesterSuffix(cfg.getTargetSemester()) + " Semester Examination, " + cfg.getAcademicSession();
        return "https://beu-bih.ac.in/result-three"
                + "?name=" + encodeValue(examName)
                + "&semester=" + encodeValue(cfg.getTargetSemester())
                + "&session=" + cfg.getAcademicSession()
                + "&regNo=" + regNo
                + "&exam_held=" + encodeValue(cfg.getPublicationCycle());
    }

    private void mergePdfArtifacts(String folderPath, String outputFileName) {
        try {
            PDFMergerUtility pdfMerger = new PDFMergerUtility();
            pdfMerger.setDestinationFileName(folderPath + File.separator + outputFileName);

            File folder = new File(folderPath);
            File[] files = folder.listFiles((dir, name) ->
                    name.endsWith(".pdf") && !name.equalsIgnoreCase(outputFileName) && !name.startsWith("ERROR_"));

            if (files != null && files.length > 0) {
                // Sort by Registration Number (Filename)
                Arrays.sort(files, Comparator.comparing(File::getName));

                for (File file : files) {
                    pdfMerger.addSource(file);
                }
                pdfMerger.mergeDocuments(null);
                LOG.info("Merged Document Created: {}", outputFileName);
            }
        } catch (IOException e) {
            LOG.error("Failed to merge PDF artifacts", e);
        }
    }

    private void captureErrorState(Page page, String outputDir, long regNo) {
        try {
            Path errorShot = Paths.get(outputDir, "ERROR_" + regNo + ".png");
            page.screenshot(new Page.ScreenshotOptions().setPath(errorShot));
        } catch (Exception ignored) {}
    }

    private String encodeValue(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }

    private String resolveSemesterSuffix(String sem) {
        return switch (sem) {
            case "I" -> "1st"; case "II" -> "2nd"; case "III" -> "3rd"; case "IV" -> "4th";
            case "V" -> "5th"; case "VI" -> "6th"; case "VII" -> "7th"; case "VIII" -> "8th";
            default -> sem;
        };
    }
}