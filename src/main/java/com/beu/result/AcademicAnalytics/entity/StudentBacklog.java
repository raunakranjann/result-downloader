package com.beu.result.AcademicAnalytics.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing the record of academic arrears (Backlogs).
 * This class persists data regarding subjects that have not yet been cleared
 * by the student for each specific semester.
 */
@Entity
@Table(name = "student_backlogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentBacklog {

    /**
     * Primary Key: Registration Number.
     * Links to the central StudentResult entity.
     */
    @Id
    private Long registrationNumber;

    private String studentName;

    // ==========================================
    // OUTSTANDING SUBJECT CODES
    // ==========================================

    /**
     * The following fields store the subject codes (e.g., "100304")
     * or status flags (e.g., "FAIL") for uncleared papers.
     * * A null or empty value typically indicates no backlogs for that semester.
     */
    private String sem1;
    private String sem2;
    private String sem3;
    private String sem4;
    private String sem5;
    private String sem6;
    private String sem7;
    private String sem8;



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