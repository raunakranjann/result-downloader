package com.beu.result.beubih.service;

import com.beu.result.beubih.config.BeuBihDownloaderConfig;
import com.beu.result.beubih.util.PrintProgress;
import com.microsoft.playwright.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class BeuBihResultPrintService {

    public void printAll(BeuBihDownloaderConfig cfg) {

        // ---------------- Safety checks ----------------
        if (cfg.getExamHeld() == null || cfg.getExamHeld().isBlank()) {
            throw new IllegalArgumentException("Exam Held (Month/Year) must not be empty");
        }

        if (cfg.getOutputDir() == null || cfg.getOutputDir().isBlank()) {
            throw new IllegalArgumentException("Output directory must not be empty");
        }

        // ---------------- Progress init ----------------
        int total = (int) (cfg.getEndReg() - cfg.getStartReg() + 1);
        PrintProgress.start(total);

        // ---------------- Output directory ----------------
        File outDir = new File(cfg.getOutputDir());
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try (Playwright playwright = Playwright.create()) {

            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );

            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setViewportSize(1280, 1024)
            );

            Page page = context.newPage();

            for (long reg = cfg.getStartReg(); reg <= cfg.getEndReg(); reg++) {

                try {
                    String url = buildUrl(cfg, reg);

                    page.navigate(url, new Page.NavigateOptions().setTimeout(30000));

                    // ⏳ Angular hydration
                    page.waitForTimeout(2500);

                    if (!isValidResult(page)) {
                        System.out.println("No valid result: " + reg);
                        PrintProgress.increment();
                        continue;
                    }

                    Path pdfPath = Path.of(cfg.getOutputDir(), reg + ".pdf");

                    page.pdf(new Page.PdfOptions()
                            .setPath(pdfPath)
                            .setFormat("A4")
                            .setPrintBackground(true)
                    );

                    System.out.println("Printed: " + reg);

                } catch (Exception ex) {
                    System.out.println("Failed for " + reg + " → " + ex.getMessage());
                }

                PrintProgress.increment();
                Thread.sleep(1200);
            }

            browser.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            PrintProgress.finish();
        }
    }

    /**
     * Builds BEU-BIH result URL safely
     */
    private String buildUrl(BeuBihDownloaderConfig c, long reg) {

        String examName =
                "B.Tech. " +
                        semesterText(c.getSemester()) +
                        " Semester Examination, " +
                        c.getExamYear();

        return "https://beu-bih.ac.in/result-three"
                + "?name=" + encode(examName)
                + "&semester=" + encode(c.getSemester())
                + "&session=" + c.getExamYear()
                + "&regNo=" + reg
                + "&exam_held=" + encode(c.getExamHeld());
    }

    private String encode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }

    /**
     * Converts semester code → display text
     */
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

    /**
     * Strict BEU-BIH result validation
     */
    private boolean isValidResult(Page page) {

        if (page.locator("text=No Record Found").count() > 0) {
            return false;
        }

        Locator tables = page.locator("table");
        int count = tables.count();

        for (int i = 0; i < count; i++) {
            String text = tables.nth(i).innerText();

            if (text.contains("Subject")
                    && text.contains("Grade")
                    && text.length() > 200) {
                return true;
            }
        }
        return false;
    }
}
