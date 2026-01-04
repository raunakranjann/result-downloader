package com.beu.result.AcademicAnalytics.repository;

import com.beu.result.AcademicAnalytics.entity.StudentBacklog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data Access Object (DAO) for Student Backlogs.
 * * This repository is responsible for managing the persistence of academic arrears
 * and liability records across different semesters.
 */
@Repository
public interface StudentBacklogRepository extends JpaRepository<StudentBacklog, Long> {
    // Standard CRUD operations provided by JpaRepository
}