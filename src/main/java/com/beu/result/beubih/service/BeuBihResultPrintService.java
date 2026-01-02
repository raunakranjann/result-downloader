package com.beu.result.beubih.service;

import com.beu.result.beubih.config.BeuBihDownloaderConfig;
import com.beu.result.beubih.util.PrintProgress;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

@Service
public class BeuBihResultPrintService {

    public void printAll(BeuBihDownloaderConfig cfg) {

        int total = (int) (cfg.getEndReg() - cfg.getStartReg() + 1);
        PrintProgress.start(total);

        File outDir = new File(cfg.getOutputDir());
        if (!outDir.exists()) outDir.mkdirs();

        try (Playwright playwright = Playwright.create()) {
            // Launch browser
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 1024));
            Page page = context.newPage();

            for (long reg = cfg.getStartReg(); reg <= cfg.getEndReg(); reg++) {
                try {
                    String url = buildUrl(cfg, reg);

                    // 1. NAVIGATE
                    page.navigate(url, new Page.NavigateOptions().setTimeout(45000));

                    // 2. WAIT FOR DATA TO LOAD
                    try {
                        page.waitForSelector("text=Subject Code", new Page.WaitForSelectorOptions().setTimeout(3000));
                    } catch (Exception e) {
                        // If 'Subject Code' never appears, the page is likely broken/empty.
                    }

                    // 3. STRICT CHECK: "Is there actual data?"
                    if (!hasSubjectData(page)) {
                        System.out.println("⚠️ Skipping (No Data): " + reg);
                        PrintProgress.increment();
                        continue; // SKIP this student entirely. Do not create PDF.
                    }

                    // 4. PRINT (Only if we passed step 3)
                    Path pdfPath = Path.of(cfg.getOutputDir(), reg + ".pdf");
                    page.pdf(new Page.PdfOptions()
                            .setPath(pdfPath)
                            .setFormat("A4")
                            .setPrintBackground(true)

                    );

                    System.out.println("✅ Printed: " + reg);

                } catch (Exception ex) {
                    System.out.println("❌ Error: " + reg + " -> " + ex.getMessage());
                }

                PrintProgress.increment();
                Thread.sleep(500);
            }

            browser.close();

            // 5. MERGE (Naming fixed to match Controller)
            System.out.println("Merging generated files...");

            // FIXED LINE BELOW: Changed "Results_" to "Merged_Result_"
            String mergedFileName = "Merged_Result_" + cfg.getSemester() + "_Sem_" + cfg.getExamYear() + ".pdf";

            generateMergedPdf(cfg.getOutputDir(), mergedFileName);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            PrintProgress.finish();
        }
    }

    private boolean hasSubjectData(Page page) {
        // Step A: Check if "No Record Found" message exists
        if (page.locator("text=No Record Found").count() > 0) {
            return false;
        }

        // Step B: Find the table that contains "Subject Code"
        Locator table = page.locator("table:has-text('Subject Code')").first();

        // If the table doesn't even exist, it's invalid
        if (table.count() == 0) return false;

        // Step C: Count the rows (<tr>) inside this table
        int rowCount = table.locator("tr").count();

        if (rowCount <= 1) {
            return false; // Only header found -> EMPTY
        }

        return true; // Has data rows -> VALID
    }

    private void generateMergedPdf(String folderPath, String outputFileName) {
        try {
            PDFMergerUtility pdfMerger = new PDFMergerUtility();
            pdfMerger.setDestinationFileName(folderPath + File.separator + outputFileName);
            File folder = new File(folderPath);
            // Only merge PDF files that are NOT the output file itself
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".pdf") && !name.equalsIgnoreCase(outputFileName));

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
            case "I" -> "1st";
            case "II" -> "2nd";
            case "III" -> "3rd";
            case "IV" -> "4th";
            case "V" -> "5th";
            case "VI" -> "6th";
            case "VII" -> "7th";
            case "VIII" -> "8th";
            default -> sem;
        };
    }
}