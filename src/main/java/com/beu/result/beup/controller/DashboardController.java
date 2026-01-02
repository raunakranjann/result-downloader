package com.beu.result.beup.controller;

import com.beu.result.beup.entity.StudentResult;
import com.beu.result.beup.repository.StudentResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    @Autowired
    private StudentResultRepository resultRepository;

    @GetMapping("/dashboard")
    public String showDashboard(
            @RequestParam(name = "year", required = false) String year,
            @RequestParam(name = "branch", required = false) String branch,
            @RequestParam(name = "rank", required = false) Boolean rank,
            Model model) {

        // 1. Prepare Filter Params
        String yearPrefix = (year != null && !year.trim().isEmpty() && !year.equals("All")) ? year.trim() : null;
        // Handle "2022" -> "22" just in case, though dropdown sends "22"
        if (yearPrefix != null && yearPrefix.length() == 4) yearPrefix = yearPrefix.substring(2);

        String branchParam = (branch != null && !branch.trim().isEmpty() && !branch.equals("All")) ? branch.trim() : null;

        // 2. Fetch Data
        List<StudentResult> students;
        if (yearPrefix == null && branchParam == null) {
            students = resultRepository.findAll();
        } else {
            students = resultRepository.searchByYearAndBranch(yearPrefix, branchParam);
        }

        // 3. Sorting
        if (Boolean.TRUE.equals(rank)) {
            students = students.stream()
                    .sorted((s1, s2) -> {
                        Double cgpa1 = extractCGPA(s1);
                        Double cgpa2 = extractCGPA(s2);
                        return cgpa2.compareTo(cgpa1);
                    })
                    .collect(Collectors.toList());
        }

        // 4. Populate Model
        model.addAttribute("students", students);
        model.addAttribute("branches", resultRepository.findDistinctBranches());
        model.addAttribute("batchYears", resultRepository.findDistinctBatchYears()); // <--- NEW

        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedBranch", branch);
        model.addAttribute("isRanked", rank);

        return "dashboard";
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