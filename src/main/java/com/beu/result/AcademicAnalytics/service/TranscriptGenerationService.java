package com.beu.result.AcademicAnalytics.service;

import com.beu.result.AcademicAnalytics.entity.StudentBacklog;
import com.beu.result.AcademicAnalytics.entity.StudentGrade;
import com.beu.result.AcademicAnalytics.entity.StudentResult;
import com.beu.result.AcademicAnalytics.repository.StudentBacklogRepository;
import com.beu.result.AcademicAnalytics.repository.StudentGradeRepository;
import com.beu.result.AcademicAnalytics.repository.StudentInfoRepository;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for the automated ingestion of academic records.
 * <p>
 * This service utilizes a headless browser automation engine (Playwright) to:
 * 1. Navigate to external result portals.
 * 2. Parse unstructured DOM elements into structured Entities.
 * 3. Persist data into the centralized PostgreSQL warehouse.
 * </p>
 */
@Service
public class TranscriptGenerationService {

    private static final Logger LOG = LoggerFactory.getLogger(TranscriptGenerationService.class);

    @Autowired
    private StudentInfoRepository resultRepository;
    @Autowired
    private StudentGradeRepository gradeRepository;
    @Autowired
    private StudentBacklogRepository backlogRepository;

    @Autowired
    private DataSyncStatus syncStatus;

    /**
     * Orchestrates the batch ingestion process for a range of student IDs.
     * Executed asynchronously to prevent blocking the main application thread.
     *
     * @param urlPattern The target endpoint template.
     * @param startReg   Starting Registration ID.
     * @param endReg     Ending Registration ID.
     */
    @Async
    public void processResultRange(String urlPattern, long startReg, long endReg) {
        int totalItems = (int) (endReg - startReg) + 1;

        // Initialize Telemetry
        syncStatus.startBatch(totalItems);

        try (Playwright playwright = Playwright.create()) {
            // Launch Browser in Headless Mode (Standard for Server-Side Automation)
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );

            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions().setViewportSize(1280, 720)
            );
            Page page = context.newPage();

            for (long regNo = startReg; regNo <= endReg; regNo++) {
                String logMessage = "Ingesting Record: " + regNo;
                try {
                    String targetUrl = urlPattern.replace("{REG}", String.valueOf(regNo));

                    // Increased timeout to account for external server latency
                    page.navigate(targetUrl, new Page.NavigateOptions().setTimeout(60000));

                    // ==========================================
                    // PHASE 1: PROFILE EXTRACTION
                    // ==========================================
                    StudentResult studentProfile = null;
                    if (targetUrl.contains("beu-bih.ac.in")) {
                        studentProfile = parseModernPortalProfile(page, regNo);
                    } else {
                        studentProfile = parseLegacyPortalProfile(page, regNo);
                    }

                    if (studentProfile != null) {
                        resultRepository.save(studentProfile);

                        // ==========================================
                        // PHASE 2: ACADEMIC PERFORMANCE METRICS
                        // ==========================================
                        StudentGrade academicGrades = null;
                        if (targetUrl.contains("beu-bih.ac.in")) {
                            academicGrades = parseModernPortalGrades(page, regNo, studentProfile.getStudentName());
                        } else {
                            academicGrades = parseLegacyPortalGrades(page, regNo, studentProfile.getStudentName());
                        }

                        if (academicGrades != null) {
                            synchronizeGrades(academicGrades);
                        }

                        // ==========================================
                        // PHASE 3: LIABILITY/BACKLOG ANALYSIS
                        // ==========================================
                        String currentSem = null;
                        String statusRemarks = null;

                        if (targetUrl.contains("beu-bih.ac.in")) {
                            currentSem = extractSemesterModern(page);
                            statusRemarks = extractRemarksModern(page);
                        } else {
                            currentSem = extractSemesterLegacy(page);
                            statusRemarks = extractRemarksLegacy(page);
                        }

                        if (currentSem != null) {
                            synchronizeBacklogs(regNo, studentProfile.getStudentName(), currentSem, statusRemarks);
                        }

                        logMessage = "Successfully Indexed: " + studentProfile.getStudentName();
                        LOG.info(logMessage);
                    } else {
                        logMessage = "Skipped (Data Not Found/Invalid)";
                    }

                } catch (Exception e) {
                    logMessage = "Exception during ingestion: " + e.getMessage();
                    LOG.error(logMessage);
                } finally {
                    // Update Live Telemetry
                    syncStatus.updateProgress(logMessage);
                }
            }
            browser.close();
        } catch (Exception e) {
            LOG.error("Critical Failure in Ingestion Engine", e);
        }

        syncStatus.completeBatch();
    }

    // ==========================================
    // PERSISTENCE LOGIC (Synchronization)
    // ==========================================

    private void synchronizeBacklogs(Long regNo, String name, String semester, String remarks) {
        String valueToSave = (remarks == null || remarks.isEmpty()) ? "PASS" : remarks;

        Optional<StudentBacklog> existingOpt = backlogRepository.findById(regNo);
        StudentBacklog backlog = existingOpt.orElse(new StudentBacklog());

        backlog.setRegistrationNumber(regNo);
        backlog.setStudentName(name);

        String semClean = semester.toUpperCase().trim();

        // Map Semester Codes to Database Columns
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

    private void synchronizeGrades(StudentGrade newGrades) {
        Optional<StudentGrade> existingOpt = gradeRepository.findById(newGrades.getRegistrationNumber());
        if (existingOpt.isPresent()) {
            StudentGrade existing = existingOpt.get();
            // Compare and persist the highest score (Grade Improvement Logic)
            existing.setSem1(resolveHigherScore(existing.getSem1(), newGrades.getSem1()));
            existing.setSem2(resolveHigherScore(existing.getSem2(), newGrades.getSem2()));
            existing.setSem3(resolveHigherScore(existing.getSem3(), newGrades.getSem3()));
            existing.setSem4(resolveHigherScore(existing.getSem4(), newGrades.getSem4()));
            existing.setSem5(resolveHigherScore(existing.getSem5(), newGrades.getSem5()));
            existing.setSem6(resolveHigherScore(existing.getSem6(), newGrades.getSem6()));
            existing.setSem7(resolveHigherScore(existing.getSem7(), newGrades.getSem7()));
            existing.setSem8(resolveHigherScore(existing.getSem8(), newGrades.getSem8()));

            if (newGrades.getCgpa() != null && !newGrades.getCgpa().isEmpty() && !newGrades.getCgpa().equals("-")) {
                existing.setCgpa(newGrades.getCgpa());
            }
            gradeRepository.save(existing);
        } else {
            gradeRepository.save(newGrades);
        }
    }

    /**
     * Comparative utility to determine if the new fetched score is better than the existing one.
     * Useful for students who have attempted improvement exams.
     */
    private String resolveHigherScore(String oldVal, String newVal) {
        if (newVal == null || newVal.equals("-") || newVal.equals("NA") || newVal.isEmpty()) return oldVal;
        if (oldVal == null || oldVal.equals("-") || oldVal.equals("NA") || oldVal.isEmpty()) return newVal;
        try {
            double oldD = Double.parseDouble(oldVal);
            double newD = Double.parseDouble(newVal);
            return (newD >= oldD) ? newVal : oldVal;
        } catch (NumberFormatException e) { return newVal; }
    }

    // ==========================================
    // DOM PARSING STRATEGIES (Modern Portal)
    // ==========================================

    private String extractSemesterModern(Page page) {
        try {
            // Target: DOM Element containing "Semester: X"
            Locator semCell = page.locator("td:has-text('Semester:')").first();

            if (semCell.count() > 0) {
                String text = semCell.innerText();
                return text.replace("Semester:", "").trim();
            }
            return extractSemesterFromUrl(page);
        } catch (Exception e) { return null; }
    }

    private String extractRemarksModern(Page page) {
        try {
            // Target: CSS Class indicating failure status (text-danger)
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

    private StudentResult parseModernPortalProfile(Page page, long regNo) {
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE);
            try { page.waitForSelector("text=Student Name", new Page.WaitForSelectorOptions().setTimeout(5000)); } catch (Exception e) { return null; }

            String name = page.locator("tr:has-text('Student Name') >> td").nth(1).innerText().trim();
            Locator parentRow = page.locator("tr:has-text('Father')").first();
            String father = parentRow.locator("td").nth(1).innerText().trim();
            String mother = parentRow.locator("td").nth(3).innerText().trim();
            String rawBranch = page.locator("tr:has-text('Course Name') >> td").nth(1).innerText().trim();

            // Clean branch name by removing numeric prefixes
            String branch = rawBranch.replaceAll("^\\d+\\s*-\\s*", "").trim();
            String course = "B.Tech";

            if (name.length() > 2) return new StudentResult(regNo, name, father, mother, course, branch);
        } catch (Exception e) { }
        return null;
    }

    private StudentGrade parseModernPortalGrades(Page page, long regNo, String name) {
        try {
            Locator sgpaRow = page.locator("tr:has-text('SGPA')").last();
            if (sgpaRow.count() == 0) return null;

            List<String> cells = sgpaRow.locator("td, th").allInnerTexts();
            StudentGrade g = new StudentGrade();
            g.setRegistrationNumber(regNo);
            g.setStudentName(name);
            g.setSem1(normalizeCellData(cells, 1));
            g.setSem2(normalizeCellData(cells, 2));
            g.setSem3(normalizeCellData(cells, 3));
            g.setSem4(normalizeCellData(cells, 4));
            g.setSem5(normalizeCellData(cells, 5));
            g.setSem6(normalizeCellData(cells, 6));
            g.setSem7(normalizeCellData(cells, 7));
            g.setSem8(normalizeCellData(cells, 8));
            g.setCgpa(normalizeCellData(cells, 9));
            return g;
        } catch (Exception e) { return null; }
    }

    // ==========================================
    // DOM PARSING STRATEGIES (Legacy Portal)
    // ==========================================

    private String extractRemarksLegacy(Page page) {
        try {
            String val = getTextById(page, "#ContentPlaceHolder1_DataList3_remarkLabel_0");
            return (val == null || val.isEmpty()) ? "PASS" : val;
        } catch (Exception e) { return "PASS"; }
    }

    private String extractSemesterLegacy(Page page) {
        try {
            Locator semLoc = page.locator("#ContentPlaceHolder1_DataList2_Exam_Name_0");
            if (semLoc.count() > 0) return semLoc.innerText().trim();
        } catch (Exception e) {}
        return null;
    }

    private StudentResult parseLegacyPortalProfile(Page page, long regNo) {
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

    private StudentGrade parseLegacyPortalGrades(Page page, long regNo, String name) {
        try {
            Locator table = page.locator("#ContentPlaceHolder1_GridView3");
            if (table.count() == 0) return null;
            Locator row = table.locator("tr").nth(1);
            List<String> cells = row.locator("td").allInnerTexts();

            StudentGrade g = new StudentGrade();
            g.setRegistrationNumber(regNo);
            g.setStudentName(name);
            g.setSem1(normalizeCellData(cells, 0));
            g.setSem2(normalizeCellData(cells, 1));
            g.setSem3(normalizeCellData(cells, 2));
            g.setSem4(normalizeCellData(cells, 3));
            g.setSem5(normalizeCellData(cells, 4));
            g.setSem6(normalizeCellData(cells, 5));
            g.setSem7(normalizeCellData(cells, 6));
            g.setSem8(normalizeCellData(cells, 7));
            g.setCgpa(normalizeCellData(cells, 8));
            return g;
        } catch (Exception e) { return null; }
    }

    // ==========================================
    // UTILITIES
    // ==========================================

    private String normalizeCellData(List<String> cells, int index) {
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