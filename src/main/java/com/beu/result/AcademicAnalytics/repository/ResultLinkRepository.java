package com.beu.result.AcademicAnalytics.repository;

import com.beu.result.AcademicAnalytics.entity.ResultLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultLinkRepository extends JpaRepository<ResultLink, Long> {

    Optional<ResultLink> findByLinkKey(String linkKey);

    // Fetch only enabled links
    List<ResultLink> findByIsActiveTrue();
}