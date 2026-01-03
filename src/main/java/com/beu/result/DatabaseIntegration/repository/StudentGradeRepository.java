package com.beu.result.DatabaseIntegration.repository;

import com.beu.result.DatabaseIntegration.entity.StudentGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentGradeRepository extends JpaRepository<StudentGrade, Long> {
}