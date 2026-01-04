package com.beu.result.AcademicAnalytics.repository;

import com.beu.result.AcademicAnalytics.entity.StudentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data Access Layer for Student Profiles.
 * Handles retrieval of aggregated student data, distinct batch information,
 * and dynamic search filtering.
 */
@Repository
public interface StudentInfoRepository extends JpaRepository<StudentResult, Long> {

    /**
     * Retrieves a list of all unique Engineering Branches available in the database.
     * Used to populate dropdown filters in the dashboard.
     *
     * @return List of distinct branch names sorted alphabetically.
     */
    @Query("SELECT DISTINCT s.branch FROM StudentResult s WHERE s.branch IS NOT NULL ORDER BY s.branch")
    List<String> findDistinctBranches();

    /**
     * Extracts distinct Batch Years based on the Registration Number prefix.
     * <p>
     * Logic: Parses the first 2 digits of the Registration Number (e.g., "22" from "22105...")
     * to determine the intake year.
     * </p>
     *
     * @return List of unique batch years (e.g., "21", "22", "23").
     */
    @Query(value = """
            SELECT DISTINCT SUBSTRING(CAST(registration_number AS TEXT), 1, 2) AS batch_year 
            FROM public.student_informations 
            ORDER BY batch_year
            """, nativeQuery = true)
    List<String> findDistinctBatchYears();

    /**
     * Performs a dynamic search for students based on Batch Year and Branch.
     * Handles nullable parameters to allow for optional filtering.
     *
     * @param yearPattern The prefix of the registration number (e.g., "22").
     * @param branch      The branch name or partial substring (e.g., "Computer").
     * @return A list of matching StudentResult entities.
     */
    @Query(value = """
        SELECT * FROM public.student_informations s 
        WHERE (CAST(s.registration_number AS TEXT) LIKE CONCAT(:yearPattern, '%') OR :yearPattern IS NULL)
        AND (LOWER(s.branch) LIKE LOWER(CONCAT('%', :branch, '%')) OR :branch IS NULL)
        """, nativeQuery = true)
    List<StudentResult> searchByYearAndBranch(
            @Param("yearPattern") String yearPattern,
            @Param("branch") String branch
    );
}