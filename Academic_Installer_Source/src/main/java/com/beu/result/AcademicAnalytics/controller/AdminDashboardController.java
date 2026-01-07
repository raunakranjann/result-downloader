package com.beu.result.AcademicAnalytics.controller;

import com.beu.result.AcademicAnalytics.entity.StudentInformations; // UPDATED IMPORT
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
 * Updated to use the new 'StudentInformations' entity.
 */
@Controller
@RequestMapping("/")
public class AdminDashboardController {

    private static final Logger LOG = LoggerFactory.getLogger(AdminDashboardController.class);
    private final StudentInfoRepository studentRepository;

    public AdminDashboardController(StudentInfoRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * Renders the main analytics view.
     */
    @GetMapping
    public String renderDashboard(
            @RequestParam(name = "year", required = false) String year,
            @RequestParam(name = "branch", required = false) String branch,
            Model model) {

        long startTime = System.currentTimeMillis();

        // 1. FILTER NORMALIZATION
        String yearPrefix = normalizeYearInput(year);
        String branchParam = (branch != null && !branch.trim().isEmpty() && !branch.equals("All")) ? branch.trim() : null;

        // 2. DATA RETRIEVAL (Using new Entity)
        List<StudentInformations> dataset;
        if (yearPrefix == null && branchParam == null) {
            dataset = studentRepository.findAll();
        } else {
            dataset = studentRepository.searchByYearAndBranch(yearPrefix, branchParam);
        }

        LOG.info("Dashboard query fetched {} records. Filters: [Year={}, Branch={}]",
                dataset.size(), yearPrefix, branchParam);

        // 3. KPI COMPUTATION

        // KPI A: Branch-wise Performance (Average CGPA)
        Map<String, Double> branchAvgCgpa = dataset.stream()
                .filter(s -> s.getBranch() != null && hasValidCgpa(s))
                .collect(Collectors.groupingBy(
                        StudentInformations::getBranch, // Updated Method Reference
                        Collectors.averagingDouble(s -> safeParseDouble(s.getGrade().getCgpa()))
                ));

        // KPI B: Grade Distribution Buckets
        int[] distributionBuckets = new int[5];
        for (StudentInformations s : dataset) {
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
        long passCount = dataset.stream()
                .filter(s -> hasValidCgpa(s) && safeParseDouble(s.getGrade().getCgpa()) >= 5.0)
                .count();
        long failCount = dataset.size() - passCount;

        // KPI D: Institutional Average
        double institutionalAverage = dataset.stream()
                .filter(this::hasValidCgpa)
                .mapToDouble(s -> safeParseDouble(s.getGrade().getCgpa()))
                .average().orElse(0.0);

        // 4. VIEW POPULATION
        model.addAttribute("branches", studentRepository.findDistinctBranches());
        model.addAttribute("batchYears", studentRepository.findDistinctBatchYears());

        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedBranch", branch);

        model.addAttribute("branchLabels", branchAvgCgpa.keySet());
        model.addAttribute("branchValues", branchAvgCgpa.values());
        model.addAttribute("rangeData", distributionBuckets);
        model.addAttribute("passData", Arrays.asList(passCount, failCount));

        model.addAttribute("totalStudents", dataset.size());
        model.addAttribute("avgCollegeCgpa", institutionalAverage);

        LOG.debug("Dashboard rendering completed in {}ms", System.currentTimeMillis() - startTime);

        return "dashboard";
    }

    // ==========================================
    // HELPER UTILITIES
    // ==========================================

    private String normalizeYearInput(String year) {
        if (year != null && !year.trim().isEmpty() && !year.equals("All")) {
            String trimmed = year.trim();
            return (trimmed.length() == 4) ? trimmed.substring(2) : trimmed;
        }
        return null;
    }

    private boolean hasValidCgpa(StudentInformations s) {
        return s.getGrade() != null && s.getGrade().getCgpa() != null;
    }

    private double safeParseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException | NullPointerException e) {
            return 0.0;
        }
    }
}