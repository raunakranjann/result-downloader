package com.beu.result.AcademicAnalytics.repository;

import com.beu.result.AcademicAnalytics.entity.StudentGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data Access Object (DAO) for Student Grades.
 * * This repository manages the persistence and retrieval of academic performance metrics,
 * including Semester Grade Point Averages (SGPA) and Cumulative Grade Point Averages (CGPA).
 */
@Repository
public interface StudentGradeRepository extends JpaRepository<StudentGrade, Long> {
    // Standard CRUD operations provided by JpaRepository
}