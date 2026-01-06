package com.beu.result.AcademicAnalytics.repository;

import com.beu.result.AcademicAnalytics.entity.StudentInformations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data Access Layer for Student Profiles.
 * Updated to use the new 'StudentInformations' entity.
 */
@Repository
public interface StudentInfoRepository extends JpaRepository<StudentInformations, Long> {

    /**
     * Retrieves a list of all unique Engineering Branches.
     * Updated JPQL to use the new Entity name.
     */
    @Query("SELECT DISTINCT s.branch FROM StudentInformations s WHERE s.branch IS NOT NULL ORDER BY s.branch")
    List<String> findDistinctBranches();

    /**
     * Extracts distinct Batch Years based on Registration Number prefix.
     * Table name 'student_informations' matches the @Table annotation in your entity.
     */
    @Query(value = """
            SELECT DISTINCT SUBSTRING(CAST(registration_number AS TEXT), 1, 2) AS batch_year 
            FROM public.student_informations 
            ORDER BY batch_year
            """, nativeQuery = true)
    List<String> findDistinctBatchYears();

    /**
     * Performs a dynamic search for students based on Batch Year and Branch.
     * Return type updated to StudentInformations.
     */
    @Query(value = """
        SELECT * FROM public.student_informations s 
        WHERE (CAST(s.registration_number AS TEXT) LIKE CONCAT(:yearPattern, '%') OR :yearPattern IS NULL)
        AND (LOWER(s.branch) LIKE LOWER(CONCAT('%', :branch, '%')) OR :branch IS NULL)
        """, nativeQuery = true)
    List<StudentInformations> searchByYearAndBranch(
            @Param("yearPattern") String yearPattern,
            @Param("branch") String branch
    );
}