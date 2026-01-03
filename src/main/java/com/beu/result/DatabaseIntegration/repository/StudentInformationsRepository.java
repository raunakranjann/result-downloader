package com.beu.result.DatabaseIntegration.repository;

import com.beu.result.DatabaseIntegration.entity.StudentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentInformationsRepository extends JpaRepository<StudentResult, Long> {

    // 1. Get Distinct Branches
    @Query("SELECT DISTINCT s.branch FROM StudentResult s WHERE s.branch IS NOT NULL ORDER BY s.branch")
    List<String> findDistinctBranches();

    // 2. Get Distinct Batch Years (Extract first 2 digits of Reg No)
    // CAST(registration_number AS TEXT) turns 22105... into "22105..."
    // SUBSTRING(..., 1, 2) takes "22"
    @Query(value = "SELECT DISTINCT SUBSTRING(CAST(registration_number AS TEXT), 1, 2) AS batch_year FROM public.student_informations ORDER BY batch_year", nativeQuery = true)
    List<String> findDistinctBatchYears();

    // 3. Search Query
    @Query(value = """
        SELECT * FROM public.student_informations s 
        WHERE (CAST(s.registration_number AS TEXT) LIKE :yearPattern% OR :yearPattern IS NULL)
        AND (LOWER(s.branch) LIKE LOWER(CONCAT('%', :branch, '%')) OR :branch IS NULL)
        """, nativeQuery = true)
    List<StudentResult> searchByYearAndBranch(
            @Param("yearPattern") String yearPattern,
            @Param("branch") String branch
    );
}