package com.beu.result.AcademicAnalytics.service;

import com.beu.result.AcademicAnalytics.config.ResultSourceConfig;
import com.beu.result.AcademicAnalytics.entity.StudentBacklog;
import com.beu.result.AcademicAnalytics.entity.StudentGrade;
import com.beu.result.AcademicAnalytics.entity.StudentInformations;
import com.beu.result.AcademicAnalytics.repository.StudentBacklogRepository;
import com.beu.result.AcademicAnalytics.repository.StudentGradeRepository;
import com.beu.result.AcademicAnalytics.repository.StudentInfoRepository;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for the automated ingestion of academic records.
 * Updated: Implements "High Water Mark" logic for CGPA protection.
 */
@Service
public class TranscriptGenerationService {

    private static final Logger LOG = LoggerFactory.getLogger(TranscriptGenerationService.class);

    private final StudentInfoRepository resultRepository;
    private final StudentGradeRepository gradeRepository;
    private final StudentBacklogRepository backlogRepository;
    private final DataSyncStatus syncStatus;
    private final ResultSourceConfig sourceConfig;

    public TranscriptGenerationService(StudentInfoRepository resultRepository,
                                       StudentGradeRepository gradeRepository,
                                       StudentBacklogRepository backlogRepository,
                                       DataSyncStatus syncStatus,
                                       ResultSourceConfig sourceConfig) {
        this.resultRepository = resultRepository;
        this.gradeRepository = gradeRepository;
        this.backlogRepository = backlogRepository;
        this.syncStatus = syncStatus;
        this.sourceConfig = sourceConfig;
    }

    @Async
    public void processResultRange(String linkKeyOrUrl, long startReg, long endReg) {

        String urlPattern;
        if (linkKeyOrUrl.startsWith("http")) {
            urlPattern = linkKeyOrUrl;
        } else {
            urlPattern = sourceConfig.getUrl(linkKeyOrUrl);
        }

        if (urlPattern == null) {
            LOG.error("Ingestion Aborted: Invalid Link Key or URL '{}'", linkKeyOrUrl);
            return;
        }

        int totalItems = (int) (endReg - startReg + 1);
        syncStatus.startJob(totalItems);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
            Page page = context.newPage();

            for (long regNo = startReg; regNo <= endReg; regNo++) {

                // --- CANCEL CHECK (Optional: Add here if you implement cancellation later) ---
               /* if (syncStatus.isCancelRequested()) {
                    LOG.warn("Ingestion Job Cancelled by Admin.");
                    syncStatus.updateProgress("Job Cancelled.");
                    break;
                }*/

                String logMessage = "Ingesting: " + regNo;
                try {
                    String targetUrl = urlPattern.replace("{REG}", String.valueOf(regNo));

                    // 1. ROBUST NAVIGATION
                    page.navigate(targetUrl, new Page.NavigateOptions()
                            .setTimeout(60000)
                            .setWaitUntil(WaitUntilState.NETWORKIDLE));

                    // 2. CHECK DATA STATE
                    PageStatus status = checkPageStatus(page);

                    if (status == PageStatus.READY) {
                        // --- PROCEED (Data is Complete) ---
                        boolean isLegacy = page.locator("#ContentPlaceHolder1_GridView3").count() > 0;

                        // A. Extract Semester FIRST (Needed for Logic)
                        String curSem = isLegacy ? extractSemesterLegacy(page) : extractSemesterModern(page);

                        // B. Parse Profile
                        StudentInformations profile = isLegacy
                                ? parseLegacyPortalProfile(page, regNo)
                                : parseModernPortalProfile(page, regNo);

                        if (profile != null) {
                            resultRepository.save(profile);

                            // C. Parse Grades
                            StudentGrade grades = isLegacy
                                    ? parseLegacyPortalGrades(page, regNo)
                                    : parseModernPortalGrades(page, regNo);

                            // D. Synchronize Grades with "High Water Mark" Logic
                            if (grades != null) {
                                synchronizeGrades(grades, regNo, curSem);
                            }

                            // E. Synchronize Backlogs
                            String remarks = isLegacy ? extractRemarksLegacy(page) : extractRemarksModern(page);
                            if (curSem != null) synchronizeBacklogs(regNo, curSem, remarks);

                            logMessage = "Indexed: " + profile.getStudentName();
                            LOG.info(logMessage);
                        } else {
                            logMessage = "Skipped (Parse Error): " + regNo;
                        }

                    } else if (status == PageStatus.NAME_EMPTY) {
                        logMessage = "Skipped (Name Empty): " + regNo;
                    } else if (status == PageStatus.NO_RECORD) {
                        logMessage = "Skipped (No Record): " + regNo;
                    } else if (status == PageStatus.EMPTY_TABLE) {
                        logMessage = "Skipped (Empty Table): " + regNo;
                    } else {
                        logMessage = "Skipped (Unknown State): " + regNo;
                    }

                } catch (Exception e) {
                    logMessage = "Error: " + e.getMessage();
                    LOG.error("Failed to ingest {}", regNo, e);
                } finally {
                    syncStatus.updateProgress(logMessage);
                }
            }
            browser.close();
        } catch (Exception e) {
            LOG.error("Critical Failure in Ingestion Engine", e);
        } finally {
            syncStatus.finishJob();
        }
    }

    // ==========================================
    // DATA VALIDATION LOGIC
    // ==========================================

    private enum PageStatus { READY, EMPTY_TABLE, NAME_EMPTY, NO_RECORD, UNKNOWN }

    private PageStatus checkPageStatus(Page page) {
        if (page.locator("text=/No Record Found|Invalid Registration|Data Not Available/i").count() > 0) {
            return PageStatus.NO_RECORD;
        }

        Locator nameCell;
        nameCell = page.locator("tr:has-text('Student Name') >> td").nth(1);
        if (nameCell.count() == 0) {
            nameCell = page.locator("#ContentPlaceHolder1_DataList1_StudentNameLabel_0");
        }

        if (nameCell.count() > 0) {
            String nameValue = nameCell.innerText().trim();
            if (nameValue.isEmpty() || nameValue.equals("&nbsp;")) {
                return PageStatus.NAME_EMPTY;
            }
        } else {
            return PageStatus.UNKNOWN;
        }

        Locator table = page.locator("table:has-text('Subject Code')").first();
        if (table.count() > 0) {
            int rows = table.locator("tr").count();
            if (rows > 2) {
                return PageStatus.READY;
            } else {
                return PageStatus.EMPTY_TABLE;
            }
        }

        return PageStatus.UNKNOWN;
    }

    // ==========================================
    // PERSISTENCE LOGIC (UPDATED)
    // ==========================================

    private void synchronizeGrades(StudentGrade newGrades, Long regNo, String incomingSem) {
        Optional<StudentGrade> existingOpt = gradeRepository.findById(regNo);
        StudentGrade target;

        if (existingOpt.isPresent()) {
            target = existingOpt.get();

            // 1. SGPA Logic: Always apply "New >= Old" check for every semester field
            // This satisfies "Scenario B" automatically for SGPAs
            target.setSem1(resolveHigherScore(target.getSem1(), newGrades.getSem1()));
            target.setSem2(resolveHigherScore(target.getSem2(), newGrades.getSem2()));
            target.setSem3(resolveHigherScore(target.getSem3(), newGrades.getSem3()));
            target.setSem4(resolveHigherScore(target.getSem4(), newGrades.getSem4()));
            target.setSem5(resolveHigherScore(target.getSem5(), newGrades.getSem5()));
            target.setSem6(resolveHigherScore(target.getSem6(), newGrades.getSem6()));
            target.setSem7(resolveHigherScore(target.getSem7(), newGrades.getSem7()));
            target.setSem8(resolveHigherScore(target.getSem8(), newGrades.getSem8()));

            // 2. CGPA Logic: The "High Water Mark" Rule
            // Determine max semester currently in DB
            int dbMaxSem = 0;
            if (target.getSem8() != null && !target.getSem8().equals("NA")) dbMaxSem = 8;
            else if (target.getSem7() != null && !target.getSem7().equals("NA")) dbMaxSem = 7;
            else if (target.getSem6() != null && !target.getSem6().equals("NA")) dbMaxSem = 6;
            else if (target.getSem5() != null && !target.getSem5().equals("NA")) dbMaxSem = 5;
            else if (target.getSem4() != null && !target.getSem4().equals("NA")) dbMaxSem = 4;
            else if (target.getSem3() != null && !target.getSem3().equals("NA")) dbMaxSem = 3;
            else if (target.getSem2() != null && !target.getSem2().equals("NA")) dbMaxSem = 2;
            else if (target.getSem1() != null && !target.getSem1().equals("NA")) dbMaxSem = 1;

            // Determine incoming semester index
            int incomingSemIndex = getSemesterOrder(incomingSem);

            // LOGIC: Only update CGPA if incoming sem is New (>) or Current Correction (==)
            // If incoming is Old (<), we SKIP CGPA update.
            if (incomingSemIndex >= dbMaxSem) {
                if (newGrades.getCgpa() != null && !newGrades.getCgpa().equals("NA")) {
                    target.setCgpa(newGrades.getCgpa());
                }
            }

        } else {
            // New Record: Save everything
            target = newGrades;
            StudentInformations parent = resultRepository.getReferenceById(regNo);
            target.setStudentInformations(parent);
        }
        gradeRepository.save(target);
    }

    private void synchronizeBacklogs(Long regNo, String semester, String remarks) {
        String valueToSave = (remarks == null || remarks.isEmpty()) ? "PASS" : remarks;
        String semClean = semester.toUpperCase().replaceAll("[^0-9IVX]", "");

        Optional<StudentBacklog> existingOpt = backlogRepository.findById(regNo);
        StudentBacklog backlog = existingOpt.orElse(new StudentBacklog());

        if (!existingOpt.isPresent()) {
            StudentGrade parentGrade = gradeRepository.getReferenceById(regNo);
            backlog.setStudentGrade(parentGrade);
        }

        switch (semClean) {
            case "1", "I" -> backlog.setSem1(valueToSave);
            case "2", "II" -> backlog.setSem2(valueToSave);
            case "3", "III" -> backlog.setSem3(valueToSave);
            case "4", "IV" -> backlog.setSem4(valueToSave);
            case "5", "V" -> backlog.setSem5(valueToSave);
            case "6", "VI" -> backlog.setSem6(valueToSave);
            case "7", "VII" -> backlog.setSem7(valueToSave);
            case "8", "VIII" -> backlog.setSem8(valueToSave);
        }
        backlogRepository.save(backlog);
    }

    // ==========================================
    // PARSING HELPERS
    // ==========================================

    private StudentInformations parseModernPortalProfile(Page page, long regNo) {
        try {
            String name = page.locator("tr:has-text('Student Name') >> td").nth(1).innerText().trim();
            if (name.isEmpty()) return null;

            String father = page.locator("tr:has-text('Father') >> td").nth(1).innerText().trim();
            String mother = "";
            Locator motherCell = page.locator("tr:has-text('Mother') >> td").nth(1);
            mother = motherCell.count() > 0 ? motherCell.innerText().trim() : page.locator("tr:has-text('Father') >> td").nth(3).innerText().trim();
            String branch = page.locator("tr:has-text('Course Name') >> td").nth(1).innerText().replaceAll("^\\d+\\s*-\\s*", "").trim();
            return new StudentInformations(regNo, name, father, mother, "B.Tech", branch);
        } catch (Exception e) { return null; }
    }

    private StudentGrade parseModernPortalGrades(Page page, long regNo) {
        try {
            Locator sgpaRow = page.locator("tr:has-text('SGPA')").last();
            if (sgpaRow.count() == 0) return null;
            List<String> cells = sgpaRow.locator("td, th").allInnerTexts();
            StudentGrade g = new StudentGrade();
            g.setSem1(normalize(cells, 1)); g.setSem2(normalize(cells, 2));
            g.setSem3(normalize(cells, 3)); g.setSem4(normalize(cells, 4));
            g.setSem5(normalize(cells, 5)); g.setSem6(normalize(cells, 6));
            g.setSem7(normalize(cells, 7)); g.setSem8(normalize(cells, 8));
            g.setCgpa(normalize(cells, 9));
            return g;
        } catch (Exception e) { return null; }
    }

    private StudentInformations parseLegacyPortalProfile(Page page, long regNo) {
        try {
            String name = getTextById(page, "#ContentPlaceHolder1_DataList1_StudentNameLabel_0");
            if (name == null || name.isEmpty()) return null;
            String father = getTextById(page, "#ContentPlaceHolder1_DataList1_FatherNameLabel_0");
            String mother = getTextById(page, "#ContentPlaceHolder1_DataList1_MotherNameLabel_0");
            String branch = getTextById(page, "#ContentPlaceHolder1_DataList1_CourseLabel_0");
            return new StudentInformations(regNo, name, father, mother, "B.Tech", branch);
        } catch (Exception e) { return null; }
    }

    private StudentGrade parseLegacyPortalGrades(Page page, long regNo) {
        try {
            Locator row = page.locator("#ContentPlaceHolder1_GridView3 tr").nth(1);
            if (row.count() == 0) return null;
            List<String> cells = row.locator("td").allInnerTexts();
            StudentGrade g = new StudentGrade();
            g.setSem1(normalize(cells, 0)); g.setSem2(normalize(cells, 1));
            g.setSem3(normalize(cells, 2)); g.setSem4(normalize(cells, 3));
            g.setSem5(normalize(cells, 4)); g.setSem6(normalize(cells, 5));
            g.setSem7(normalize(cells, 6)); g.setSem8(normalize(cells, 7));
            g.setCgpa(normalize(cells, 8));
            return g;
        } catch (Exception e) { return null; }
    }

    private String extractSemesterModern(Page page) {
        try {
            Locator semCell = page.locator("td:has-text('Semester:')").first();
            if (semCell.count() > 0) return semCell.innerText().replace("Semester:", "").trim();
            Matcher m = Pattern.compile("(?i)(?:semester|sem)=([IVX0-9]+)").matcher(page.url());
            return m.find() ? m.group(1) : null;
        } catch (Exception e) { return null; }
    }

    private String extractRemarksModern(Page page) {
        try {
            Locator failSpan = page.locator("span.text-danger:has-text('FAIL')").first();
            if (failSpan.count() > 0) {
                return failSpan.innerText().trim();
            }
            Locator absentSpan = page.locator("span.text-danger:has-text('ABSENT')").first();
            if (absentSpan.count() > 0) {
                return "ABSENT";
            }
            return "PASS";
        } catch (Exception e) {
            return "PASS";
        }
    }

    private String extractSemesterLegacy(Page page) {
        try {
            Locator semLoc = page.locator("#ContentPlaceHolder1_DataList2_Exam_Name_0");
            if (semLoc.count() > 0) return semLoc.innerText().trim();
        } catch (Exception e) {}
        return null;
    }

    private String extractRemarksLegacy(Page page) {
        try {
            String val = getTextById(page, "#ContentPlaceHolder1_DataList3_remarkLabel_0");
            return (val != null && !val.isEmpty()) ? val : "PASS";
        } catch (Exception e) { return "PASS"; }
    }

    // ==========================================
    // UTILITIES
    // ==========================================

    private String resolveHigherScore(String oldVal, String newVal) {
        if (newVal == null || newVal.equals("NA") || newVal.equals("-")) return oldVal;
        if (oldVal == null || oldVal.equals("NA") || oldVal.equals("-")) return newVal;
        try { return (Double.parseDouble(newVal) >= Double.parseDouble(oldVal)) ? newVal : oldVal; }
        catch (NumberFormatException e) { return newVal; }
    }

    private String normalize(List<String> cells, int index) {
        if (index < cells.size()) {
            String val = cells.get(index).trim();
            return (val.equals("-") || val.equals("NA") || val.isEmpty()) ? "NA" : val;
        } return "NA";
    }

    private String getTextById(Page page, String selector) {
        Locator loc = page.locator(selector);
        return (loc.count() > 0) ? loc.innerText().trim() : null;
    }

    private int getSemesterOrder(String sem) {
        if (sem == null) return 0;
        switch (sem.trim().toUpperCase()) {
            case "I": case "1": return 1;
            case "II": case "2": return 2;
            case "III": case "3": return 3;
            case "IV": case "4": return 4;
            case "V": case "5": return 5;
            case "VI": case "6": return 6;
            case "VII": case "7": return 7;
            case "VIII": case "8": return 8;
            default: return 0;
        }
    }
}