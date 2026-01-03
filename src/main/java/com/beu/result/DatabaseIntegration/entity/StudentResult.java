package com.beu.result.DatabaseIntegration.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_informations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentResult {

    @Id
    private Long registrationNumber;

    private String studentName;
    private String fatherName;  // New Field
    private String motherName;  // New Field
    private String course;
    private String branch;


    // --- JOINS ---

    // Join with Grades Table
    // "mappedBy" tells Hibernate that the Grade table shares the same Primary Key
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "registrationNumber", referencedColumnName = "registrationNumber")
    private StudentGrade grade;

    // Join with Backlogs Table
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "registrationNumber", referencedColumnName = "registrationNumber")
    private StudentBacklog backlog;


    // --- MANUAL CONSTRUCTOR FOR SCRAPER ---
    // This allows the scraper to create the object without needing to know about Grade/Backlog yet
    public StudentResult(Long registrationNumber, String studentName, String fatherName, String motherName, String course, String branch) {
        this.registrationNumber = registrationNumber;
        this.studentName = studentName;
        this.fatherName = fatherName;
        this.motherName = motherName;
        this.course = course;
        this.branch = branch;
    }

}