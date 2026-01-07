package com.beu.result.AcademicAnalytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "result_links")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String linkKey; // This is the "Key"

    @Column(nullable = false, length = 2048)
    private String urlTemplate;

    private String description;

    private boolean isActive = true;

    // --- CONSTRUCTOR FOR SEEDING DATA ---
    public ResultLink(String linkKey, String urlTemplate, boolean isActive) {
        this.linkKey = linkKey;
        this.urlTemplate = urlTemplate;
        this.description = linkKey; // Default description to Key
        this.isActive = isActive;
    }
}