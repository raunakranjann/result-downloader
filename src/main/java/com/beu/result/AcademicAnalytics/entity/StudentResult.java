package com.beu.result.AcademicAnalytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Core Entity representing the Student Profile.
 * This class serves as the root for the academic data aggregate,
 * holding personal details and linking to Grades and Backlog history.
 */
@Entity
@Table(name = "student_informations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentResult {

    @Id
    private Long registrationNumber;

    private String studentName;
    private String fatherName;
    private String motherName;
    private String course;
    private String branch;

    // ==========================================
    // RELATIONSHIP MAPPINGS
    // ==========================================

    /**
     * Association with StudentGrade.
     * Fetched EAGERly to ensure grade data is available immediately upon profile retrieval.
     */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "registrationNumber", referencedColumnName = "registrationNumber")
    private StudentGrade grade;

    /**
     * Association with StudentBacklog.
     * Linked via Registration Number to track academic history.
     */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "registrationNumber", referencedColumnName = "registrationNumber")
    private StudentBacklog backlog;

    // ==========================================
    // DATA INGESTION CONSTRUCTORS
    // ==========================================

    /**
     * Specialized constructor for the Data Ingestion Service.
     * Allows for the initialization of the student profile during the parsing phase
     * before relational data (Grades/Backlogs) is fully linked.
     *
     * @param registrationNumber Unique Student ID
     * @param studentName        Name of the student
     * @param fatherName         Guardian/Father Name
     * @param motherName         Guardian/Mother Name
     * @param course             Enrolled Course (e.g., B.Tech)
     * @param branch             Specialization (e.g., CSE)
     */
    public StudentResult(Long registrationNumber, String studentName, String fatherName,
                         String motherName, String course, String branch) {
        this.registrationNumber = registrationNumber;
        this.studentName = studentName;
        this.fatherName = fatherName;
        this.motherName = motherName;
        this.course = course;
        this.branch = branch;
    }
}