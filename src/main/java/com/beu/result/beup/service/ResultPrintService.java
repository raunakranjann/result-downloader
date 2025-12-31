package com.beu.result.beup.service;

import com.microsoft.playwright.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;

@Service
public class ResultPrintService {

    public void printAllResults(
            String semester,
            String examType,      // REGULAR / BACKLOG
            int examYear,
            long startReg,
            long endReg,
            String outputDir
    ) {

        File outDir = new File(outputDir);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try (Playwright playwright = Playwright.create()) {

            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );

            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions().setViewportSize(1280, 1024)
            );

            Page page = context.newPage();

            for (long regNo = startReg; regNo <= endReg; regNo++) {

                String url = buildUrl(semester, examType, examYear, regNo);

                try {
                    page.navigate(url, new Page.NavigateOptions().setTimeout(30000));
                    page.waitForTimeout(1500);

                    if (!isValidResult(page)) {
                        System.out.println("No valid result: " + regNo);
                        continue;
                    }

                    Path pdfPath = Path.of(outputDir, regNo + ".pdf");

                    page.pdf(new Page.PdfOptions()
                            .setPath(pdfPath)
                            .setFormat("A4")
                            .setPrintBackground(true)
                    );

                    System.out.println("Printed: " + regNo);
                    Thread.sleep(1200);

                } catch (Exception e) {
                    System.out.println("Failed for " + regNo + " → " + e.getMessage());
                }
            }

            browser.close();
        }
    }

    /**
     * 🔥 Universal BEU URL builder
     */
    private String buildUrl(String semester, String examType, int examYear, long regNo) {

        String semText = semesterText(semester);
        String semParam = semester;
        String batchYear = extractBatchYear(regNo);

        String suffix = examType.equalsIgnoreCase("BACKLOG")
                ? "_old_B" + batchYear
                : "_B" + batchYear;

        return "https://results.beup.ac.in/ResultsBTech"
                + semText
                + "Sem"
                + examYear
                + suffix
                + "Pub.aspx?Sem="
                + semParam
                + "&RegNo="
                + regNo;
    }

    /**
     * Converts semester Roman → English
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
            default -> throw new IllegalArgumentException("Invalid semester");
        };
    }

    /**
     * Extracts batch year from registration number
     * 22105123021 → B2022
     */
    private String extractBatchYear(long regNo) {
        String reg = String.valueOf(regNo);
        return "20" + reg.substring(0, 2);
    }

    /**
     * Strict BEU result validation
     */
    private boolean isValidResult(Page page) {

        if (page.locator("text=No Record Found").count() > 0) {
            return false;
        }

        Locator tables = page.locator("table");
        int count = tables.count();

        for (int i = 0; i < count; i++) {
            Locator table = tables.nth(i);
            String text = table.innerText();

            if (text.contains("Subject Code")
                    && text.contains("Grade")
                    && table.locator("tr").count() > 3) {
                return true;
            }
        }
        return false;
    }
}
