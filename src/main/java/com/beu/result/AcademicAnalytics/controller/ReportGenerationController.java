package com.beu.result.AcademicAnalytics.controller;

import com.beu.result.AcademicAnalytics.entity.StudentResult;
import com.beu.result.AcademicAnalytics.repository.StudentInfoRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Report Generation and Data Export.
 * <p>
 * Handles the detailed "Student Registry" view with pagination, search, and sorting capabilities.
 * Also manages the export of filtered datasets to Excel (XLSX) format for offline analysis.
 * </p>
 */
@Controller
@RequestMapping("/reports")
public class ReportGenerationController {

    private static final Logger LOG = LoggerFactory.getLogger(ReportGenerationController.class);
    private final StudentInfoRepository studentRepository;

    public ReportGenerationController(StudentInfoRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    // ==========================================
    // 1. PAGINATED REGISTRY VIEW
    // ==========================================

    /**
     * Renders the Paginated Student Registry.
     * Endpoint: /reports/student-registry
     */
    @GetMapping("/student-registry")
    public String viewStudentRegistry(
            @RequestParam(name = "year", required = false) String year,
            @RequestParam(name = "branch", required = false) String branch,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "rank", required = false) Boolean rank,
            @RequestParam(name = "showBacklog", required = false) Boolean showBacklog,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Model model) {

        long startTime = System.currentTimeMillis();

        // 1. Retrieve & Filter Dataset
        List<StudentResult> filteredDataset = executeFilterQuery(year, branch, name, rank);

        // 2. Pagination Logic
        int totalItems = filteredDataset.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);

        // Normalize page request
        if (page < 1) page = 1;
        if (page > totalPages && totalPages > 0) page = totalPages;

        int startIdx = (page - 1) * size;
        int endIdx = Math.min(startIdx + size, totalItems);

        List<StudentResult> pageContent;
        if (startIdx >= totalItems) {
            pageContent = Collections.emptyList();
        } else {
            pageContent = filteredDataset.subList(startIdx, endIdx);
        }

        // 3. View Attributes Population
        model.addAttribute("students", pageContent);

        // Context Filters (Dropdowns)
        model.addAttribute("branches", studentRepository.findDistinctBranches());
        model.addAttribute("batchYears", studentRepository.findDistinctBatchYears());

        // Preserve Filter State
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedBranch", branch);
        model.addAttribute("selectedName", name);
        model.addAttribute("isRanked", rank);
        model.addAttribute("showBacklog", showBacklog != null && showBacklog);

        // Pagination Meta-data
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("pageSize", size);

        LOG.debug("Registry view loaded. Page {}/{} ({}ms)", page, totalPages, System.currentTimeMillis() - startTime);

        return "student-registry"; // Rename 'show-data.html' to 'student-registry.html'
    }

    // ==========================================
    // 2. DATA EXPORT (EXCEL)
    // ==========================================

    /**
     * Generates and downloads an Excel report of the current filtered view.
     * Endpoint: /reports/export/excel
     */
    @GetMapping("/export/excel")
    public void exportToExcel(
            @RequestParam(name = "year", required = false) String year,
            @RequestParam(name = "branch", required = false) String branch,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "rank", required = false) Boolean rank,
            HttpServletResponse response) throws IOException {

        LOG.info("Initiating Excel export. Filters: [Year={}, Branch={}, Rank={}]", year, branch, rank);

        List<StudentResult> dataset = executeFilterQuery(year, branch, name, rank);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=academic_registry_export.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Academic Registry");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // Create Headers
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Reg No", "Student Name", "Branch", "Sem 1", "Sem 2", "Sem 3", "Sem 4", "Sem 5", "Sem 6", "Sem 7", "Sem 8", "CGPA"};

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Populate Data
            int rowIdx = 1;
            for (StudentResult s : dataset) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(s.getRegistrationNumber());
                row.createCell(1).setCellValue(s.getStudentName());
                row.createCell(2).setCellValue(s.getBranch());

                if (s.getGrade() != null) {
                    row.createCell(3).setCellValue(s.getGrade().getSem1());
                    row.createCell(4).setCellValue(s.getGrade().getSem2());
                    row.createCell(5).setCellValue(s.getGrade().getSem3());
                    row.createCell(6).setCellValue(s.getGrade().getSem4());
                    row.createCell(7).setCellValue(s.getGrade().getSem5());
                    row.createCell(8).setCellValue(s.getGrade().getSem6());
                    row.createCell(9).setCellValue(s.getGrade().getSem7());
                    row.createCell(10).setCellValue(s.getGrade().getSem8());
                    row.createCell(11).setCellValue(s.getGrade().getCgpa());
                }
            }

            // Auto-size columns for readability
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
            LOG.info("Excel export completed successfully. Rows: {}", dataset.size());
        }
    }

    // ==========================================
    // HELPER: FILTERING ENGINE
    // ==========================================

    private List<StudentResult> executeFilterQuery(String year, String branch, String name, Boolean rank) {
        // 1. Normalize Parameters
        String yearPrefix = (year != null && !year.trim().isEmpty() && !year.equals("All")) ? year.trim() : null;
        if (yearPrefix != null && yearPrefix.length() == 4) yearPrefix = yearPrefix.substring(2);

        String branchParam = (branch != null && !branch.trim().isEmpty() && !branch.equals("All")) ? branch.trim() : null;

        // 2. Database Retrieval
        List<StudentResult> results;
        if (yearPrefix == null && branchParam == null) {
            results = studentRepository.findAll();
        } else {
            results = studentRepository.searchByYearAndBranch(yearPrefix, branchParam);
        }

        // 3. In-Memory Name Filter
        if (name != null && !name.trim().isEmpty()) {
            String searchName = name.trim().toLowerCase();
            results = results.stream()
                    .filter(s -> s.getStudentName() != null && s.getStudentName().toLowerCase().contains(searchName))
                    .collect(Collectors.toList());
        }

        // 4. Ranking Logic (Sort by CGPA Descending)
        if (Boolean.TRUE.equals(rank)) {
            results = results.stream()
                    .sorted((s1, s2) -> {
                        Double cgpa1 = safeParseDouble(s1.getGrade() != null ? s1.getGrade().getCgpa() : "0");
                        Double cgpa2 = safeParseDouble(s2.getGrade() != null ? s2.getGrade().getCgpa() : "0");
                        return cgpa2.compareTo(cgpa1);
                    })
                    .collect(Collectors.toList());
        }
        return results;
    }

    private Double safeParseDouble(String val) {
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException | NullPointerException e) {
            return 0.0;
        }
    }
}