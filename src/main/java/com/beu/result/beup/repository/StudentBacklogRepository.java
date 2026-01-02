package com.beu.result.beup.repository;

import com.beu.result.beup.entity.StudentBacklog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentBacklogRepository extends JpaRepository<StudentBacklog, Long> {
}