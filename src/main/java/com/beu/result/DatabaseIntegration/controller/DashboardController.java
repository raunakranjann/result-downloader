package com.beu.result.DatabaseIntegration.controller;

import com.beu.result.DatabaseIntegration.entity.StudentResult;
import com.beu.result.DatabaseIntegration.repository.StudentInformationsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller responsible for rendering the main analytics dashboard.
 * It processes student data to generate statistics like average CGPA,
 * pass/fail ratios, and grade distribution charts, supporting dynamic filtering.
 */
@Controller
@RequestMapping("/")
public class DashboardController {

    @Autowired
    private StudentInformationsRepository resultRepository;

    /**
     * Retrieves student records (filtered by year/branch if requested), calculates Key Performance Indicators (KPIs),
     * and prepares data structures for visualization charts in the view.
     */
    @GetMapping
    public String showAnalytics(
            @RequestParam(name = "year", required = false) String year,
            @RequestParam(name = "branch", required = false) String branch,
            Model model) {

        /*
         * Block 1: Prepare Filter Parameters
         * Handle "All" selection and normalize year input (e.g., "2022" -> "22").
         */
        String yearPrefix = (year != null && !year.trim().isEmpty() && !year.equals("All")) ? year.trim() : null;
        if (yearPrefix != null && yearPrefix.length() == 4) {
            yearPrefix = yearPrefix.substring(2);
        }

        String branchParam = (branch != null && !branch.trim().isEmpty() && !branch.equals("All")) ? branch.trim() : null;

        /*
         * Block 2: Fetch Filtered Data
         * Retrieves the list of students based on the active filters.
         * If no filters are active, retrieves the complete dataset.
         */
        List<StudentResult> filteredStudents;
        if (yearPrefix == null && branchParam == null) {
            filteredStudents = resultRepository.findAll();
        } else {
            filteredStudents = resultRepository.searchByYearAndBranch(yearPrefix, branchParam);
        }

        /*
         * Block 3: Calculate Statistics (Based on Filtered Data)
         */

        // 3a. Average CGPA per Branch
        Map<String, Double> branchAvgCgpa = filteredStudents.stream()
                .filter(s -> s.getBranch() != null && s.getGrade() != null && s.getGrade().getCgpa() != null)
                .collect(Collectors.groupingBy(
                        StudentResult::getBranch,
                        Collectors.averagingDouble(s -> parseCgpa(s.getGrade().getCgpa()))
                ));

        // 3b. CGPA Distribution Ranges
        int[] ranges = new int[5]; // [0]:<6, [1]:6-7, [2]:7-8, [3]:8-9, [4]:9-10
        for (StudentResult s : filteredStudents) {
            if (s.getGrade() != null && s.getGrade().getCgpa() != null) {
                double cgpa = parseCgpa(s.getGrade().getCgpa());
                if (cgpa >= 9.0) ranges[4]++;
                else if (cgpa >= 8.0) ranges[3]++;
                else if (cgpa >= 7.0) ranges[2]++;
                else if (cgpa >= 6.0) ranges[1]++;
                else ranges[0]++;
            }
        }

        // 3c. Pass vs Fail Ratio
        long passCount = filteredStudents.stream()
                .filter(s -> s.getGrade() != null && parseCgpa(s.getGrade().getCgpa()) >= 5.0)
                .count();
        long failCount = filteredStudents.size() - passCount;

        /*
         * Block 4: Populate Model Attributes
         * Sends processed stats, filter options, and current selection state to the view.
         */
        // Dropdown Options
        model.addAttribute("branches", resultRepository.findDistinctBranches());
        model.addAttribute("batchYears", resultRepository.findDistinctBatchYears());

        // Current Selection
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedBranch", branch);

        // Chart Data
        model.addAttribute("branchLabels", branchAvgCgpa.keySet());
        model.addAttribute("branchValues", branchAvgCgpa.values());
        model.addAttribute("rangeData", ranges);
        model.addAttribute("passData", Arrays.asList(passCount, failCount));

        // Single Value KPIs
        model.addAttribute("totalStudents", filteredStudents.size());
        model.addAttribute("avgCollegeCgpa", filteredStudents.stream()
                .filter(s -> s.getGrade() != null)
                .mapToDouble(s -> parseCgpa(s.getGrade().getCgpa()))
                .average().orElse(0.0));

        return "dashboard";
    }

    /**
     * Helper utility to safely parse a CGPA string into a double.
     * Returns 0.0 if the string is invalid or null.
     */
    private double parseCgpa(String cgpaStr) {
        try {
            return Double.parseDouble(cgpaStr);
        } catch (Exception e) {
            return 0.0;
        }
    }
}