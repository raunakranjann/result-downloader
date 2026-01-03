package com.beu.result.DatabaseIntegration.repository;

import com.beu.result.DatabaseIntegration.entity.StudentBacklog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentBacklogRepository extends JpaRepository<StudentBacklog, Long> {
}