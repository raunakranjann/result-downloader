package com.beu.result.AcademicAnalytics.controller;

import com.beu.result.AcademicAnalytics.entity.StudentResult;
import com.beu.result.AcademicAnalytics.repository.StudentInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Primary Controller for the Business Intelligence (BI) Dashboard.
 * <p>
 * This controller orchestrates the aggregation of academic data to visualize
 * Key Performance Indicators (KPIs) such as CGPA trends, pass/fail ratios,
 * and branch-wise performance metrics.
 * </p>
 */
@Controller
@RequestMapping("/")
public class AdminDashboardController {

    private static final Logger LOG = LoggerFactory.getLogger(AdminDashboardController.class);
    private final StudentInfoRepository studentRepository;

    // Best Practice: Constructor Injection ensures dependencies are immutable and not null.
    public AdminDashboardController(StudentInfoRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * Renders the main analytics view.
     * <p>
     * Logic Flow:
     * 1. Resolve Filter Parameters (Year/Branch).
     * 2. Fetch specific dataset from the Warehouse.
     * 3. Compute in-memory statistics for visualization.
     * </p>
     *
     * @param year   Optional filter for Batch Year (e.g., "2022" or "22").
     * @param branch Optional filter for Engineering Branch.
     */
    @GetMapping
    public String renderDashboard(
            @RequestParam(name = "year", required = false) String year,
            @RequestParam(name = "branch", required = false) String branch,
            Model model) {

        long startTime = System.currentTimeMillis();

        // ==========================================
        // 1. FILTER NORMALIZATION
        // ==========================================
        // Convert full year "2022" to short code "22" for the database query
        String yearPrefix = normalizeYearInput(year);
        String branchParam = (branch != null && !branch.trim().isEmpty() && !branch.equals("All")) ? branch.trim() : null;

        // ==========================================
        // 2. DATA RETRIEVAL
        // ==========================================
        List<StudentResult> dataset;
        if (yearPrefix == null && branchParam == null) {
            dataset = studentRepository.findAll();
        } else {
            dataset = studentRepository.searchByYearAndBranch(yearPrefix, branchParam);
        }

        LOG.info("Dashboard query fetched {} records. Filters: [Year={}, Branch={}]",
                dataset.size(), yearPrefix, branchParam);

        // ==========================================
        // 3. KPI COMPUTATION (Business Logic)
        // ==========================================

        // KPI A: Branch-wise Performance (Average CGPA)
        Map<String, Double> branchAvgCgpa = dataset.stream()
                .filter(s -> s.getBranch() != null && hasValidCgpa(s))
                .collect(Collectors.groupingBy(
                        StudentResult::getBranch,
                        Collectors.averagingDouble(s -> safeParseDouble(s.getGrade().getCgpa()))
                ));

        // KPI B: Grade Distribution Buckets (Histogram Data)
        // Buckets: [0]:<6, [1]:6-7, [2]:7-8, [3]:8-9, [4]:9-10
        int[] distributionBuckets = new int[5];
        for (StudentResult s : dataset) {
            if (hasValidCgpa(s)) {
                double cgpa = safeParseDouble(s.getGrade().getCgpa());
                if (cgpa >= 9.0) distributionBuckets[4]++;
                else if (cgpa >= 8.0) distributionBuckets[3]++;
                else if (cgpa >= 7.0) distributionBuckets[2]++;
                else if (cgpa >= 6.0) distributionBuckets[1]++;
                else distributionBuckets[0]++;
            }
        }

        // KPI C: Pass/Fail Ratio
        // Business Rule: CGPA >= 5.0 is considered "Active/Passing" for aggregate stats
        long passCount = dataset.stream()
                .filter(s -> hasValidCgpa(s) && safeParseDouble(s.getGrade().getCgpa()) >= 5.0)
                .count();
        long failCount = dataset.size() - passCount;

        // KPI D: Institutional Average
        double institutionalAverage = dataset.stream()
                .filter(this::hasValidCgpa)
                .mapToDouble(s -> safeParseDouble(s.getGrade().getCgpa()))
                .average().orElse(0.0);

        // ==========================================
        // 4. VIEW POPULATION
        // ==========================================

        // Filter Dropdowns
        model.addAttribute("branches", studentRepository.findDistinctBranches());
        model.addAttribute("batchYears", studentRepository.findDistinctBatchYears());

        // Preserve Selection State
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedBranch", branch);

        // Visualization Data (Used by Chart.js/ApexCharts)
        model.addAttribute("branchLabels", branchAvgCgpa.keySet());
        model.addAttribute("branchValues", branchAvgCgpa.values());
        model.addAttribute("rangeData", distributionBuckets);
        model.addAttribute("passData", Arrays.asList(passCount, failCount));

        // Summary Cards
        model.addAttribute("totalStudents", dataset.size());
        model.addAttribute("avgCollegeCgpa", institutionalAverage);

        LOG.debug("Dashboard rendering completed in {}ms", System.currentTimeMillis() - startTime);

        return "dashboard"; // Maps to resources/templates/dashboard.html
    }

    // ==========================================
    // HELPER UTILITIES
    // ==========================================

    /**
     * Normalizes year input to match database schema (e.g., "2022" -> "22").
     */
    private String normalizeYearInput(String year) {
        if (year != null && !year.trim().isEmpty() && !year.equals("All")) {
            String trimmed = year.trim();
            // If user sends "2022", return "22". If "22", return "22".
            return (trimmed.length() == 4) ? trimmed.substring(2) : trimmed;
        }
        return null;
    }

    /**
     * Checks if a student has a non-null Grade object and a non-null CGPA string.
     */
    private boolean hasValidCgpa(StudentResult s) {
        return s.getGrade() != null && s.getGrade().getCgpa() != null;
    }

    /**
     * Safely parses a string to double, handling non-numeric values (e.g., "Pending", "Absent").
     * Returns 0.0 on failure.
     */
    private double safeParseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException | NullPointerException e) {
            return 0.0;
        }
    }
}