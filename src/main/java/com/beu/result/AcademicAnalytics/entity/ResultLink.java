package com.beu.result.AcademicAnalytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "result_links") // No schema="public", perfect for local DB
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String linkKey;

    @Column(nullable = false, length = 2048)
    private String urlTemplate;

    private String description;

    private boolean isActive = true;

    // Seeding constructor
    public ResultLink(String linkKey, String urlTemplate, boolean isActive) {
        this.linkKey = linkKey;
        this.urlTemplate = urlTemplate;
        this.description = linkKey;
        this.isActive = isActive;
    }
}