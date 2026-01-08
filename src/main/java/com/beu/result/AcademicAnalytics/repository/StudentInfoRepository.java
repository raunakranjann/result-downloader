package com.beu.result.AcademicAnalytics.repository;

import com.beu.result.AcademicAnalytics.entity.StudentInformations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentInfoRepository extends JpaRepository<StudentInformations, Long> {

    @Query("SELECT DISTINCT s.branch FROM StudentInformations s WHERE s.branch IS NOT NULL ORDER BY s.branch")
    List<String> findDistinctBranches();

    /**
     * Updated for SQLite:
     * 1. Removed 'public.' prefix.
     * 2. Switched 'SUBSTRING' to 'substr'.
     * 3. Casting to TEXT is simplified.
     */
    @Query(value = """
            SELECT DISTINCT substr(CAST(registration_number AS TEXT), 1, 2) AS batch_year 
            FROM student_informations 
            ORDER BY batch_year
            """, nativeQuery = true)
    List<String> findDistinctBatchYears();

    /**
     * Updated for SQLite:
     * 1. Removed 'public.' prefix.
     * 2. Used the '||' operator for string concatenation (SQLite standard).
     * 3. Simplified NULL handling.
     */
    @Query(value = """
        SELECT * FROM student_informations s 
        WHERE (CAST(s.registration_number AS TEXT) LIKE (:yearPattern || '%') OR :yearPattern IS NULL)
        AND (LOWER(s.branch) LIKE LOWER('%' || :branch || '%') OR :branch IS NULL)
        """, nativeQuery = true)
    List<StudentInformations> searchByYearAndBranch(
            @Param("yearPattern") String yearPattern,
            @Param("branch") String branch
    );
}