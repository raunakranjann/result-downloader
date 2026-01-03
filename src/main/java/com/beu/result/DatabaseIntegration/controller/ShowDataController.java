package com.beu.result.DatabaseIntegration.controller;

import com.beu.result.DatabaseIntegration.entity.StudentResult;
import com.beu.result.DatabaseIntegration.repository.StudentInformationsRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ShowDataController {

    @Autowired
    private StudentInformationsRepository resultRepository;

    // --- 1. SHOW DATA WITH PAGINATION ---
    @GetMapping("/show-data")
    public String showDashboard(
            @RequestParam(name = "year", required = false) String year,
            @RequestParam(name = "branch", required = false) String branch,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "rank", required = false) Boolean rank,
            @RequestParam(name = "page", defaultValue = "1") int page, // Default Page 1
            @RequestParam(name = "size", defaultValue = "20") int size, // Default 20 records per page
            Model model) {

        // 1. Get ALL Filtered & Sorted Data
        List<StudentResult> allStudents = getFilteredList(year, branch, name, rank);

        // 2. Pagination Logic
        int totalItems = allStudents.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);

        // Clamp page number
        if (page < 1) page = 1;
        if (page > totalPages && totalPages > 0) page = totalPages;

        int start = (page - 1) * size;
        int end = Math.min(start + size, totalItems);

        List<StudentResult> paginatedList;
        if (start > totalItems) {
            paginatedList = Collections.emptyList();
        } else {
            paginatedList = allStudents.subList(start, end);
        }

        // 3. Populate Model
        model.addAttribute("students", paginatedList);
        model.addAttribute("branches", resultRepository.findDistinctBranches());
        model.addAttribute("batchYears", resultRepository.findDistinctBatchYears());

        // Pass Filters back to UI
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedBranch", branch);
        model.addAttribute("selectedName", name);
        model.addAttribute("isRanked", rank);

        // Pass Pagination Info
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("pageSize", size);

        return "show-data";
    }

    // --- 2. DOWNLOAD EXCEL ENDPOINT ---
    @GetMapping("/download-excel")
    public void downloadExcel(
            @RequestParam(name = "year", required = false) String year,
            @RequestParam(name = "branch", required = false) String branch,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "rank", required = false) Boolean rank,
            HttpServletResponse response) throws IOException {

        List<StudentResult> students = getFilteredList(year, branch, name, rank);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=students_record.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Students");

            // Header Row
            Row header = sheet.createRow(0);
            String[] headers = {"Reg No", "Name", "Branch", "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "CGPA"};

            // Bold Font for Header
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            int rowIdx = 1;
            for (StudentResult s : students) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(s.getRegistrationNumber());
                row.createCell(1).setCellValue(s.getStudentName());
                row.createCell(2).setCellValue(s.getBranch());

                if(s.getGrade() != null) {
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

            workbook.write(response.getOutputStream());
        }
    }

    // --- HELPER: Shared Filtering Logic ---
    private List<StudentResult> getFilteredList(String year, String branch, String name, Boolean rank) {
        // 1. Prepare Params
        String yearPrefix = (year != null && !year.trim().isEmpty() && !year.equals("All")) ? year.trim() : null;
        if (yearPrefix != null && yearPrefix.length() == 4) yearPrefix = yearPrefix.substring(2);

        String branchParam = (branch != null && !branch.trim().isEmpty() && !branch.equals("All")) ? branch.trim() : null;

        // 2. Fetch Base List
        List<StudentResult> students;
        if (yearPrefix == null && branchParam == null) {
            students = resultRepository.findAll();
        } else {
            students = resultRepository.searchByYearAndBranch(yearPrefix, branchParam);
        }

        // 3. Filter by Name
        if (name != null && !name.trim().isEmpty()) {
            String searchName = name.trim().toLowerCase();
            students = students.stream()
                    .filter(s -> s.getStudentName() != null && s.getStudentName().toLowerCase().contains(searchName))
                    .collect(Collectors.toList());
        }

        // 4. Sort by Rank
        if (Boolean.TRUE.equals(rank)) {
            students = students.stream()
                    .sorted((s1, s2) -> {
                        Double cgpa1 = extractCGPA(s1);
                        Double cgpa2 = extractCGPA(s2);
                        return cgpa2.compareTo(cgpa1);
                    })
                    .collect(Collectors.toList());
        }
        return students;
    }

    private Double extractCGPA(StudentResult s) {
        if (s.getGrade() == null || s.getGrade().getCgpa() == null) return 0.0;
        try {
            return Double.parseDouble(s.getGrade().getCgpa());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}