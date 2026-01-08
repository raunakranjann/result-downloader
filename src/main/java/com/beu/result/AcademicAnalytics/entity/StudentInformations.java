package com.beu.result.AcademicAnalytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Root Entity.
 * Contains personal details and the Primary Key (Registration Number).
 */
@Entity
// Ensure NO schema="public" is present here
@Table(name = "student_informations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentInformations {

    @Id
    private Long registrationNumber;

    private String studentName;
    private String fatherName;
    private String motherName;
    private String course;
    private String branch;

    /**
     * Link to StudentGrade.
     * In SQLite, this One-to-One relationship will be managed by Hibernate
     * without needing complex PostgreSQL-specific schema constraints.
     */
    @OneToOne(mappedBy = "studentInformations", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumn
    private StudentGrade grade;

    // Ingestion Constructor
    public StudentInformations(Long registrationNumber, String studentName, String fatherName,
                               String motherName, String course, String branch) {
        this.registrationNumber = registrationNumber;
        this.studentName = studentName;
        this.fatherName = fatherName;
        this.motherName = motherName;
        this.course = course;
        this.branch = branch;
    }
}