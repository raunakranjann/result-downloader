package com.beu.result.AcademicAnalytics.repository;

import com.beu.result.AcademicAnalytics.entity.StudentGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Student Grades.
 * Handles persistence of SGPA/CGPA data.
 */
@Repository
public interface StudentGradeRepository extends JpaRepository<StudentGrade, Long> {
    // No custom queries needed yet; findById and save are sufficient.
}