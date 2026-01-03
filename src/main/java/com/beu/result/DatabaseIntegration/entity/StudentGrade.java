package com.beu.result.DatabaseIntegration.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_grades")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentGrade {

    @Id
    private Long registrationNumber;

    private String studentName;

    private String sem1;
    private String sem2;
    private String sem3;
    private String sem4;
    private String sem5;
    private String sem6;
    private String sem7;
    private String sem8;

    private String cgpa; // "Average"
}