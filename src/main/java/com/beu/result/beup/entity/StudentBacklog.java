package com.beu.result.beup.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_backlogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentBacklog {

    @Id
    private Long registrationNumber;
    private String studentName;

    // Stores codes like "100304" or "FAIL: 100304"
    private String sem1;
    private String sem2;
    private String sem3;
    private String sem4;
    private String sem5;
    private String sem6;
    private String sem7;
    private String sem8;
}