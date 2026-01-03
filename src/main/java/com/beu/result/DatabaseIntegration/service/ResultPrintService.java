package com.beu.result.DatabaseIntegration.service;

import com.beu.result.DatabaseIntegration.entity.StudentBacklog;
import com.beu.result.DatabaseIntegration.entity.StudentGrade;
import com.beu.result.DatabaseIntegration.entity.StudentResult;
import com.beu.result.DatabaseIntegration.repository.StudentBacklogRepository;
import com.beu.result.DatabaseIntegration.repository.StudentGradeRepository;
import com.beu.result.DatabaseIntegration.repository.StudentInformationsRepository;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResultPrintService {

    @Autowired
    private StudentInformationsRepository resultRepository;
    @Autowired
    private StudentGradeRepository gradeRepository;
    @Autowired
    private StudentBacklogRepository backlogRepository;

    @Autowired
    private ScrapingState state;

    @Async
    public void processResultRange(String urlPattern, long startReg, long endReg) {
        int totalItems = (int) (endReg - startReg) + 1;
        state.start(totalItems);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );

            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions().setViewportSize(1280, 720)
            );
            Page page = context.newPage();

            for (long regNo = startReg; regNo <= endReg; regNo++) {
                String logMessage = "Processing " + regNo;
                try {
                    String targetUrl = urlPattern.replace("{REG}", String.valueOf(regNo));
                    page.navigate(targetUrl, new Page.NavigateOptions().setTimeout(60000));

                    // --- 1. Basic Info ---
                    StudentResult info = null;
                    if (targetUrl.contains("beu-bih.ac.in")) {
                        info = scrapeAngularBasicInfo(page, regNo);
                    } else {
                        info = scrapeOldAspNetInfo(page, regNo);
                    }

                    if (info != null) {
                        resultRepository.save(info);

                        // --- 2. Grades ---
                        StudentGrade newGrades = null;
                        if (targetUrl.contains("beu-bih.ac.in")) {
                            newGrades = scrapeAngularGrades(page, regNo, info.getStudentName());
                        } else {
                            newGrades = scrapeOldAspNetGrades(page, regNo, info.getStudentName());
                        }

                        if (newGrades != null) {
                            saveOrUpdateGrades(newGrades);
                        }

                        // --- 3. Backlogs (FIXED) ---
                        String currentSem = null;
                        String failRemarks = null;

                        if (targetUrl.contains("beu-bih.ac.in")) {
                            currentSem = extractSemesterAngular(page); // Uses screenshot logic
                            failRemarks = extractRemarksAngular(page); // Uses text-danger class
                        } else {
                            currentSem = extractSemesterOld(page);
                            failRemarks = extractRemarksOld(page);
                        }

                        if (currentSem != null) {
                            saveOrUpdateBacklogs(regNo, info.getStudentName(), currentSem, failRemarks);
                        }

                        logMessage = "Saved: " + info.getStudentName();
                        System.out.println(logMessage);
                    } else {
                        logMessage = "Skipped (Invalid/No Data)";
                    }

                } catch (Exception e) {
                    logMessage = "Error: " + e.getMessage();
                } finally {
                    state.increment(logMessage);
                }
            }
            browser.close();
        }
        state.finish();
    }

    // ==========================================
    // BACKLOG SAVING LOGIC
    // ==========================================
    private void saveOrUpdateBacklogs(Long regNo, String name, String semester, String remarks) {
        String valueToSave = (remarks == null || remarks.isEmpty()) ? "PASS" : remarks;

        Optional<StudentBacklog> existingOpt = backlogRepository.findById(regNo);
        StudentBacklog backlog = existingOpt.orElse(new StudentBacklog());

        backlog.setRegistrationNumber(regNo);
        backlog.setStudentName(name);

        String semClean = semester.toUpperCase().trim();

        // Match Roman Numerals (I, II, III) or Numbers (1, 2, 3)
        if (semClean.equals("I") || semClean.equals("1")) backlog.setSem1(valueToSave);
        else if (semClean.equals("II") || semClean.equals("2")) backlog.setSem2(valueToSave);
        else if (semClean.equals("III") || semClean.equals("3")) backlog.setSem3(valueToSave);
        else if (semClean.equals("IV") || semClean.equals("4")) backlog.setSem4(valueToSave);
        else if (semClean.equals("V") || semClean.equals("5")) backlog.setSem5(valueToSave);
        else if (semClean.equals("VI") || semClean.equals("6")) backlog.setSem6(valueToSave);
        else if (semClean.equals("VII") || semClean.equals("7")) backlog.setSem7(valueToSave);
        else if (semClean.equals("VIII") || semClean.equals("8")) backlog.setSem8(valueToSave);

        backlogRepository.save(backlog);
    }

    // ==========================================
    // EXTRACTION HELPERS (Updated based on Screenshots)
    // ==========================================

    private String extractSemesterAngular(Page page) {
        try {
            // Strategy: Look for the specific table cell from image_d29944.png
            // HTML: <td><strong>Semester:</strong> III</td>
            Locator semCell = page.locator("td:has-text('Semester:')").first();

            if (semCell.count() > 0) {
                String text = semCell.innerText();
                // text will be "Semester: III"
                return text.replace("Semester:", "").trim();
            }

            // Backup: Check URL
            return extractSemesterFromUrl(page);
        } catch (Exception e) { return null; }
    }

    private String extractRemarksAngular(Page page) {
        try {
            // Strategy: Look for red text class from image_d291e0.png
            // HTML: <span class="fw-bold text-danger">FAIL: 100304</span>
            Locator redText = page.locator("span.text-danger").first();

            if (redText.count() > 0) {
                String text = redText.innerText().trim();
                if (text.contains("FAIL")) {
                    return text;
                }
            }
        } catch (Exception e) {}
        return "PASS";
    }

    private String extractSemesterFromUrl(Page page) {
        try {
            String url = page.url();
            Pattern p = Pattern.compile("(?i)(?:semester|sem)=([IVX0-9]+)");
            Matcher m = p.matcher(url);
            if (m.find()) return m.group(1);
        } catch (Exception e) {}
        return null;
    }

    // ==========================================
    // OLD SITE EXTRACTORS
    // ==========================================
    private String extractRemarksOld(Page page) {
        try {
            // ID from image_d22125.png
            String val = getTextById(page, "#ContentPlaceHolder1_DataList3_remarkLabel_0");
            return (val == null || val.isEmpty()) ? "PASS" : val;
        } catch (Exception e) { return "PASS"; }
    }

    private String extractSemesterOld(Page page) {
        try {
            // ID from image_d22125.png (Top table, Semester: I)
            Locator semLoc = page.locator("#ContentPlaceHolder1_DataList2_Exam_Name_0");
            if (semLoc.count() > 0) return semLoc.innerText().trim();
        } catch (Exception e) {}
        return null;
    }

    // ==========================================
    // EXISTING CODE PRESERVATION
    // ==========================================

    private void saveOrUpdateGrades(StudentGrade newGrades) {
        Optional<StudentGrade> existingOpt = gradeRepository.findById(newGrades.getRegistrationNumber());
        if (existingOpt.isPresent()) {
            StudentGrade existing = existingOpt.get();
            existing.setSem1(getBetterScore(existing.getSem1(), newGrades.getSem1()));
            existing.setSem2(getBetterScore(existing.getSem2(), newGrades.getSem2()));
            existing.setSem3(getBetterScore(existing.getSem3(), newGrades.getSem3()));
            existing.setSem4(getBetterScore(existing.getSem4(), newGrades.getSem4()));
            existing.setSem5(getBetterScore(existing.getSem5(), newGrades.getSem5()));
            existing.setSem6(getBetterScore(existing.getSem6(), newGrades.getSem6()));
            existing.setSem7(getBetterScore(existing.getSem7(), newGrades.getSem7()));
            existing.setSem8(getBetterScore(existing.getSem8(), newGrades.getSem8()));
            if (newGrades.getCgpa() != null && !newGrades.getCgpa().isEmpty() && !newGrades.getCgpa().equals("-")) {
                existing.setCgpa(newGrades.getCgpa());
            }
            gradeRepository.save(existing);
        } else {
            gradeRepository.save(newGrades);
        }
    }

    private String getBetterScore(String oldVal, String newVal) {
        if (newVal == null || newVal.equals("-") || newVal.equals("NA") || newVal.isEmpty()) return oldVal;
        if (oldVal == null || oldVal.equals("-") || oldVal.equals("NA") || oldVal.isEmpty()) return newVal;
        try {
            double oldD = Double.parseDouble(oldVal);
            double newD = Double.parseDouble(newVal);
            return (newD >= oldD) ? newVal : oldVal;
        } catch (NumberFormatException e) { return newVal; }
    }

    private StudentResult scrapeAngularBasicInfo(Page page, long regNo) {
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE);
            try { page.waitForSelector("text=Student Name", new Page.WaitForSelectorOptions().setTimeout(5000)); } catch (Exception e) { return null; }
            String name = page.locator("tr:has-text('Student Name') >> td").nth(1).innerText().trim();
            Locator parentRow = page.locator("tr:has-text('Father')").first();
            String father = parentRow.locator("td").nth(1).innerText().trim();
            String mother = parentRow.locator("td").nth(3).innerText().trim();
            String rawBranch = page.locator("tr:has-text('Course Name') >> td").nth(1).innerText().trim();
            String branch = rawBranch.replaceAll("^\\d+\\s*-\\s*", "").trim();
            String course = "B.Tech";
            if (name.length() > 2) return new StudentResult(regNo, name, father, mother, course, branch);
        } catch (Exception e) { }
        return null;
    }

    private StudentGrade scrapeAngularGrades(Page page, long regNo, String name) {
        try {
            Locator sgpaRow = page.locator("tr:has-text('SGPA')").last();
            if (sgpaRow.count() == 0) return null;
            List<String> cells = sgpaRow.locator("td, th").allInnerTexts();
            StudentGrade g = new StudentGrade();
            g.setRegistrationNumber(regNo);
            g.setStudentName(name);
            g.setSem1(getCell(cells, 1));
            g.setSem2(getCell(cells, 2));
            g.setSem3(getCell(cells, 3));
            g.setSem4(getCell(cells, 4));
            g.setSem5(getCell(cells, 5));
            g.setSem6(getCell(cells, 6));
            g.setSem7(getCell(cells, 7));
            g.setSem8(getCell(cells, 8));
            g.setCgpa(getCell(cells, 9));
            return g;
        } catch (Exception e) { return null; }
    }

    private StudentResult scrapeOldAspNetInfo(Page page, long regNo) {
        try {
            String name = getTextById(page, "#ContentPlaceHolder1_DataList1_StudentNameLabel_0");
            if (name == null) return null;
            String father = getTextById(page, "#ContentPlaceHolder1_DataList1_FatherNameLabel_0");
            String mother = getTextById(page, "#ContentPlaceHolder1_DataList1_MotherNameLabel_0");
            String branch = getTextById(page, "#ContentPlaceHolder1_DataList1_CourseLabel_0");
            String course = "B.Tech";
            return new StudentResult(regNo, name, father, mother, course, branch);
        } catch (Exception e) { return null; }
    }

    private StudentGrade scrapeOldAspNetGrades(Page page, long regNo, String name) {
        try {
            Locator table = page.locator("#ContentPlaceHolder1_GridView3");
            if (table.count() == 0) return null;
            Locator row = table.locator("tr").nth(1);
            List<String> cells = row.locator("td").allInnerTexts();
            StudentGrade g = new StudentGrade();
            g.setRegistrationNumber(regNo);
            g.setStudentName(name);
            g.setSem1(getCell(cells, 0));
            g.setSem2(getCell(cells, 1));
            g.setSem3(getCell(cells, 2));
            g.setSem4(getCell(cells, 3));
            g.setSem5(getCell(cells, 4));
            g.setSem6(getCell(cells, 5));
            g.setSem7(getCell(cells, 6));
            g.setSem8(getCell(cells, 7));
            g.setCgpa(getCell(cells, 8));
            return g;
        } catch (Exception e) { return null; }
    }

    private String getCell(List<String> cells, int index) {
        if (index < cells.size()) {
            String val = cells.get(index).trim();
            return (val.equals("-") || val.equals("NA") || val.isEmpty()) ? null : val;
        }
        return null;
    }

    private String getTextById(Page page, String selector) {
        Locator loc = page.locator(selector);
        return (loc.count() > 0) ? loc.innerText().trim() : null;
    }
}