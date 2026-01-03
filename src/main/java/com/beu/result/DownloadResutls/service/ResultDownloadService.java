package com.beu.result.DownloadResutls.service;

import com.beu.result.DownloadResutls.config.BeuBihDownloaderConfig;
import com.beu.result.DownloadResutls.util.PrintProgress;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;

@Service
public class ResultDownloadService {

    public void printAll(BeuBihDownloaderConfig cfg) {

        int total = (int) (cfg.getEndReg() - cfg.getStartReg() + 1);
        PrintProgress.start(total);

        File outDir = new File(cfg.getOutputDir());
        if (!outDir.exists()) outDir.mkdirs();

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));

            // User-Agent to look like a real Chrome browser
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1280, 1024)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));

            for (long reg = cfg.getStartReg(); reg <= cfg.getEndReg(); reg++) {

                Page page = context.newPage();
                boolean isSuccess = false;

                // --- RETRY LOOP ---
                for (int attempt = 1; attempt <= 3; attempt++) {
                    try {
                        String url = buildUrl(cfg, reg);

                        // 1. Navigate & Wait for Network Idle
                        page.navigate(url, new Page.NavigateOptions()
                                .setTimeout(60000)
                                .setWaitUntil(WaitUntilState.NETWORKIDLE));

                        // 2. Wait a tiny bit for the "Subject Code" header (Template) to render
                        try {
                            page.waitForSelector("text=Subject Code", new Page.WaitForSelectorOptions().setTimeout(5000));
                        } catch (TimeoutError e) {
                            // Ignored: If it times out, the checks below will determine if it's a blank page
                        }

                        // --- LOGIC START ---

                        // Check A: Explicit "No Record Found" message (Valid Skip)
                        if (page.locator("text=No Record Found").count() > 0) {
                            System.out.println("⚠️ Skipping (No Record): " + reg);
                            isSuccess = true;
                            break; // Done, don't retry
                        }

                        // Check B: Does the Table Exist?
                        Locator table = page.locator("table:has-text('Subject Code')").first();

                        if (table.count() > 0) {
                            // Table Exists: Now check if it has data or is empty
                            int rowCount = table.locator("tr").count();

                            if (rowCount > 1) {
                                // CASE: Full Data -> PRINT
                                Path pdfPath = Path.of(cfg.getOutputDir(), reg + ".pdf");
                                page.pdf(new Page.PdfOptions()
                                        .setPath(pdfPath)
                                        .setFormat("A3") // Kept A3 as per your snippet
                                        .setPrintBackground(true)
                                );
                                System.out.println("✅ Printed: " + reg);
                                isSuccess = true;
                                break; // Done
                            } else {
                                // CASE: Table exists but has <= 1 row (Header only) -> EMPTY TABLE
                                // Treat as "No Data" and SKIP (Do NOT Retry)
                                System.out.println("⚠️ Skipping (Empty Table): " + reg);
                                isSuccess = true;
                                break; // Done
                            }
                        } else {
                            // CASE: Table does NOT exist at all (Blank Page / Server Error)
                            // This is the "No Empty Table" condition -> RETRY
                            if (attempt < 3) {
                                System.out.println("🔄 Retry " + attempt + " for " + reg + " (Page content missing)...");
                                Thread.sleep(2000);
                            }
                        }

                    } catch (Exception ex) {
                        System.out.println("❌ Error (Attempt " + attempt + "): " + reg + " -> " + ex.getMessage());
                    }
                }

                // If it still fails (No table found after 3 attempts), take a Screenshot
                if (!isSuccess) {
                    System.out.println("❌ Failed: " + reg);
                    try {
                        Path errorShot = Paths.get(cfg.getOutputDir(), "ERROR_" + reg + ".png");
                        page.screenshot(new Page.ScreenshotOptions().setPath(errorShot));
                        System.out.println("📸 Saved screenshot: " + errorShot.toString());
                    } catch (Exception e) {
                        System.out.println("Could not save screenshot.");
                    }
                }

                page.close();
                PrintProgress.increment();
            }

            context.close();
            browser.close();

            System.out.println("Merging generated files...");
            String mergedFileName = "Merged_Result_" + cfg.getSemester() + "_Sem_" + cfg.getExamYear() + ".pdf";
            generateMergedPdf(cfg.getOutputDir(), mergedFileName);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            PrintProgress.finish();
        }
    }

    // Unused now (logic moved inline), but kept for potential external calls
    private boolean hasSubjectData(Page page) {
        Locator table = page.locator("table:has-text('Subject Code')").first();
        if (table.count() == 0) return false;
        return table.locator("tr").count() > 1;
    }

    private void generateMergedPdf(String folderPath, String outputFileName) {
        try {
            PDFMergerUtility pdfMerger = new PDFMergerUtility();
            pdfMerger.setDestinationFileName(folderPath + File.separator + outputFileName);
            File folder = new File(folderPath);
            File[] files = folder.listFiles((dir, name) ->
                    name.endsWith(".pdf") && !name.equalsIgnoreCase(outputFileName));

            if (files != null && files.length > 0) {
                Arrays.sort(files, Comparator.comparing(File::getName));
                for (File file : files) pdfMerger.addSource(file);
                pdfMerger.mergeDocuments(null);
                System.out.println("✅ SUCCESSFULLY MERGED: " + outputFileName);
            }
        } catch (IOException e) {
            System.err.println("❌ Error merging: " + e.getMessage());
        }
    }

    private String buildUrl(BeuBihDownloaderConfig c, long reg) {
        String examName = "B.Tech. " + semesterText(c.getSemester()) + " Semester Examination, " + c.getExamYear();
        return "https://beu-bih.ac.in/result-three"
                + "?name=" + encode(examName)
                + "&semester=" + encode(c.getSemester())
                + "&session=" + c.getExamYear()
                + "&regNo=" + reg
                + "&exam_held=" + encode(c.getExamHeld());
    }

    private String encode(String val) { return URLEncoder.encode(val, StandardCharsets.UTF_8); }

    private String semesterText(String sem) {
        return switch (sem) {
            case "I" -> "1st"; case "II" -> "2nd"; case "III" -> "3rd"; case "IV" -> "4th";
            case "V" -> "5th"; case "VI" -> "6th"; case "VII" -> "7th"; case "VIII" -> "8th";
            default -> sem;
        };
    }
}