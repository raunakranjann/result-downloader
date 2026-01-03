package com.beu.result.DownloadResutls.config;

/**
 * Configuration object for BEU-BIH result printing.
 * Populated from Thymeleaf form submission.
 */
public class BeuBihDownloaderConfig {

    /** Semester: I, II, III, IV, V, VI, VII, VIII */
    private String semester;

    /** Examination year (center year in URL): 2024 */
    private Integer examYear;

    /** Exam held month/year: July/2025, May/2025 */
    private String examHeld;

    /** Registration number range */
    private Long startReg;
    private Long endReg;

    /** Output directory for PDFs */
    private String outputDir;

    /* ===================== GETTERS & SETTERS ===================== */

    public String getSemester() {
        return semester == null ? "I" : semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public Integer getExamYear() {
        return examYear == null ? 2024 : examYear;
    }

    public void setExamYear(Integer examYear) {
        this.examYear = examYear;
    }

    /**
     * IMPORTANT:
     * Never return null — prevents NPE during URL build
     */
    public String getExamHeld() {
        return (examHeld == null || examHeld.isBlank())
                ? "July/2025"
                : examHeld.trim();
    }

    public void setExamHeld(String examHeld) {
        this.examHeld = examHeld;
    }

    public Long getStartReg() {
        return startReg;
    }

    public void setStartReg(Long startReg) {
        this.startReg = startReg;
    }

    public Long getEndReg() {
        return endReg;
    }

    public void setEndReg(Long endReg) {
        this.endReg = endReg;
    }

    public String getOutputDir() {
        return (outputDir == null || outputDir.isBlank())
                ? "D:/beu-bih-results"
                : outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }
}
