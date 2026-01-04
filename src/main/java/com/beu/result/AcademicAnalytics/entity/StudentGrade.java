package com.beu.result.AcademicAnalytics.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing the academic performance record of a student.
 * This entity persists Semester Grade Point Averages (SGPA) for all semesters
 * and the computed Cumulative Grade Point Average (CGPA).
 */
@Entity
@Table(name = "student_grades")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentGrade {

    /**
     * Primary Key: Registration Number.
     * Serves as the foreign key link to the central StudentResult profile.
     */
    @Id
    private Long registrationNumber;

    private String studentName;

    // ==========================================
    // SEMESTER PERFORMANCE METRICS (SGPA)
    // ==========================================

    private String sem1;
    private String sem2;
    private String sem3;
    private String sem4;
    private String sem5;
    private String sem6;
    private String sem7;
    private String sem8;

    /**
     * Cumulative Grade Point Average (CGPA).
     * Represents the aggregated academic standing.
     */
    private String cgpa;


    /**
     * Helper method to access semester data dynamically.
     * Reduces complexity in the Thymeleaf view.
     */
    public String getSem(int semester) {
        switch (semester) {
            case 1: return sem1;
            case 2: return sem2;
            case 3: return sem3;
            case 4: return sem4;
            case 5: return sem5;
            case 6: return sem6;
            case 7: return sem7;
            case 8: return sem8;
            default: return null;
        }
    }



}