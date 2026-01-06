package com.beu.result.DocumentArchival.service;

import com.beu.result.AcademicAnalytics.config.ResultSourceConfig;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

@Service
public class CertificateGenerationService {

    private static final Logger LOG = LoggerFactory.getLogger(CertificateGenerationService.class);

    private final ArchivalTelemetry telemetry;
    private final ResultSourceConfig sourceConfig;

    public CertificateGenerationService(ArchivalTelemetry telemetry, ResultSourceConfig sourceConfig) {
        this.telemetry = telemetry;
        this.sourceConfig = sourceConfig;
    }

    @Async
    public void generateCertificates(ArchivalJobRequest jobRequest) {

        String urlTemplate = sourceConfig.getUrl(jobRequest.getLinkKey());
        if (urlTemplate == null) {
            LOG.error("ABORTING: No configuration found for Data Source '{}'", jobRequest.getLinkKey());
            return;
        }

        long startReg = jobRequest.getRangeStart();
        long endReg = jobRequest.getRangeEnd();
        int totalRecords = (int) (endReg - startReg + 1);

        telemetry.initializeJob(totalRecords);
        // LOG.info("Starting Archival Batch..."); // Cleaner logs

        String safeBatchName = jobRequest.getLinkKey().replaceAll("[^a-zA-Z0-9.-]", "_");
        File outputDir = Paths.get(jobRequest.getStorageLocation(), safeBatchName).toFile();
        if (!outputDir.exists()) outputDir.mkdirs();

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            // Standard A4-ish viewport for consistent PDF rendering
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1280, 1024));

            Page page = context.newPage();

            for (long currentReg = startReg; currentReg <= endReg; currentReg++) {
                processSingleRecord(page, urlTemplate, currentReg, outputDir.getAbsolutePath());
            }

            context.close();
            browser.close();

            // Merge Logic
            String mergedFileName = "Merged_Transcript_" + safeBatchName + ".pdf";
            LOG.info("Initiating Merge Sequence inside folder: {}", outputDir.getName());
            mergePdfArtifacts(outputDir.getAbsolutePath(), mergedFileName);

        } catch (Exception e) {
            LOG.error("Critical Failure in Archival Engine", e);
        } finally {
            telemetry.finalizeJob();
            LOG.info("Archival Job Terminated.");
        }
    }

    private void processSingleRecord(Page page, String urlTemplate, long regNo, String outputDir) {
        String statusMessage;
        boolean success = false;

        try {
            String targetUrl = urlTemplate.replace("{REG}", String.valueOf(regNo));

            // 1. FAST NAVIGATION (COMMIT)
            page.navigate(targetUrl, new Page.NavigateOptions()
                    .setTimeout(45000)
                    .setWaitUntil(WaitUntilState.COMMIT));

            // 2. SMART WAIT FOR COMPLETENESS
            PageStatus status = waitForDataCompleteness(page);

            if (status == PageStatus.READY) {
                // --- SUCCESS: DATA FOUND ---
                Path pdfPath = Paths.get(outputDir, regNo + ".pdf");
                printDynamicPdf(page, pdfPath);

                LOG.info("[Record {}] Archived Successfully.", regNo);
                statusMessage = "Archived: " + regNo;
                success = true;

            } else if (status == PageStatus.NO_RECORD) {
                statusMessage = "Skipped (No Record): " + regNo;
            } else if (status == PageStatus.EMPTY) {
                statusMessage = "Skipped (Empty Table): " + regNo;
            } else if (status == PageStatus.NAME_EMPTY) {
                statusMessage = "Skipped (Name Empty): " + regNo;
            } else {
                // Timeout / Unknown
                captureErrorState(page, outputDir, regNo);
                statusMessage = "Failed (Timeout): " + regNo;
            }

        } catch (Exception e) {
            LOG.error("[Record {}] Error: {}", regNo, e.getMessage());
            statusMessage = "Error: " + regNo;
            captureErrorState(page, outputDir, regNo);
        }

        telemetry.updateStatus(statusMessage);
    }

    // ==========================================
    // DATA COMPLETENESS LOGIC (Shared Logic)
    // ==========================================

    private enum PageStatus { READY, EMPTY, NAME_EMPTY, NO_RECORD, LOADING }

    private PageStatus waitForDataCompleteness(Page page) {
        long startTime = System.currentTimeMillis();
        long timeout = 15000; // 15 Seconds Max Wait

        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                // 1. Explicit Failure
                if (page.locator("text=/No Record Found|Invalid Registration|Data Not Available/i").count() > 0) {
                    return PageStatus.NO_RECORD;
                }

                // 2. Table Check
                Locator table = page.locator("table:has-text('Subject Code')").first();
                boolean tableHasData = false;
                if (table.count() > 0) {
                    if (table.locator("tr").count() > 2) {
                        tableHasData = true;
                    } else {
                        // Headers only -> Empty
                        return PageStatus.EMPTY;
                    }
                }

                // 3. Name Check
                boolean nameExists = false;
                Locator nameCell = page.locator("tr:has-text('Student Name') >> td").nth(1);
                if (nameCell.count() == 0) {
                    // Legacy Selector
                    nameCell = page.locator("#ContentPlaceHolder1_DataList1_StudentNameLabel_0");
                }

                if (nameCell.count() > 0) {
                    String nameText = nameCell.innerText().trim();
                    if (!nameText.isEmpty() && !nameText.equals("&nbsp;")) {
                        nameExists = true;
                    } else {
                        // Name cell exists but is blank -> Bad Data
                        return PageStatus.NAME_EMPTY;
                    }
                }

                // 4. Decision
                if (nameExists && tableHasData) {
                    return PageStatus.READY;
                } else if (nameExists || (table.count() > 0 && !tableHasData)) {
                    // Partial Load -> Wait
                    Thread.sleep(500);
                    continue;
                } else {
                    // Nothing yet -> Wait
                    Thread.sleep(500);
                    continue;
                }

            } catch (Exception e) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
        }
        return PageStatus.LOADING;
    }

    // ==========================================
    // PDF UTILITIES
    // ==========================================

    private void printDynamicPdf(Page page, Path pdfPath) {
        // Calculate exact dimensions to fit content without whitespace
        Object dimensions = page.evaluate("() => { " +
                "  const body = document.body;" +
                "  const html = document.documentElement;" +
                "  return {" +
                "    width: Math.max(body.scrollWidth, body.offsetWidth, html.clientWidth) + 'px'," +
                "    height: Math.max(body.scrollHeight, body.offsetHeight, html.clientHeight) + 'px'" +
                "  };" +
                "}");

        @SuppressWarnings("unchecked")
        Map<String, String> dimMap = (Map<String, String>) dimensions;

        page.addStyleTag(new Page.AddStyleTagOptions().setContent(
                "@media print { body { margin: 0; padding: 10px; } @page { margin: 0; } }"
        ));

        page.pdf(new Page.PdfOptions()
                .setPath(pdfPath)
                .setWidth(dimMap.get("width"))
                .setHeight(dimMap.get("height"))
                .setPrintBackground(true)
        );
    }

    private void mergePdfArtifacts(String folderPath, String outputFileName) {
        try {
            PDFMergerUtility pdfMerger = new PDFMergerUtility();
            pdfMerger.setDestinationFileName(folderPath + File.separator + outputFileName);
            File folder = new File(folderPath);
            File[] files = folder.listFiles((dir, name) ->
                    name.endsWith(".pdf") && !name.equalsIgnoreCase(outputFileName) && !name.startsWith("ERROR_"));

            if (files != null && files.length > 0) {
                Arrays.sort(files, Comparator.comparing(File::getName));
                for (File file : files) pdfMerger.addSource(file);
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
}