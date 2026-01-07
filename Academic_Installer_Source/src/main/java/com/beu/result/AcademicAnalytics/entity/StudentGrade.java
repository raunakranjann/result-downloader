package com.beu.result.AcademicAnalytics.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "student_grades")
@Data
@NoArgsConstructor
public class StudentGrade {

    @Id
    private Long id; // Will be same as StudentInformations ID

    // ==========================================
    // LINK TO PARENT (StudentInformations)
    // ==========================================
    @OneToOne
    @MapsId // Copies ID from StudentInformations
    @JoinColumn(name = "registration_number")
    @ToString.Exclude
    private StudentInformations studentInformations;

    // ==========================================
    // LINK TO CHILD (StudentBacklog)
    // ==========================================
    @OneToOne(mappedBy = "studentGrade", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumn
    private StudentBacklog backlog;

    // ==========================================
    // DATA FIELDS
    // ==========================================
    private String sem1;
    private String sem2;
    private String sem3;
    private String sem4;
    private String sem5;
    private String sem6;
    private String sem7;
    private String sem8;
    private String cgpa;

    public String getSem(int semester) {
        switch (semester) {
            case 1: return sem1; case 2: return sem2; case 3: return sem3;
            case 4: return sem4; case 5: return sem5; case 6: return sem6;
            case 7: return sem7; case 8: return sem8; default: return null;
        }
    }
}