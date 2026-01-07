package com.beu.result.AcademicAnalytics.config;

import com.beu.result.AcademicAnalytics.entity.ResultLink;
import com.beu.result.AcademicAnalytics.repository.ResultLinkRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dynamic Configuration Registry.
 * Fetches URL templates from the Database (Table: result_links).
 */
@Component
public class ResultSourceConfig {

    private final ResultLinkRepository repository;

    // Inject Repository
    public ResultSourceConfig(ResultLinkRepository repository) {
        this.repository = repository;
    }

    /**
     * Seeds the database with the initial hardcoded links if the table is empty.
     * This ensures you don't lose your existing data configuration.
     */
    @PostConstruct
    public void seedInitialData() {
        if (repository.count() == 0) {
            System.out.println("Seeding initial Result Links into Database...");

            // Batch 2022-26
            repository.save(new ResultLink("5th Sem (22-26)", "https://results.beup.ac.in/ResultsBTech5thSem2024_B2022Pub.aspx?Sem=V&RegNo={REG}", true));
            repository.save(new ResultLink("4th Sem (22-26)", "https://results.beup.ac.in/ResultsBTech4thSem2024_B2022Pub.aspx?Sem=IV&RegNo={REG}", true));
            repository.save(new ResultLink("3rd Sem (22-26)", "https://results.beup.ac.in/ResultsBTech3rdSem2023_B2022Pub.aspx?Sem=III&RegNo={REG}", true));
            repository.save(new ResultLink("2nd Sem (22-26)", "https://results.beup.ac.in/ResultsBTech2ndSem2023_B2022Pub.aspx?Sem=II&RegNo={REG}", true));
            repository.save(new ResultLink("1st Sem (22-26)", "https://results.beup.ac.in/ResultsBTech1stSem2022_B2022Pub.aspx?Sem=I&RegNo={REG}", true));

            // Batch 2023-27
            repository.save(new ResultLink("3rd Sem (23-27)", "https://beu-bih.ac.in/result-three?name=B.Tech.%203rd%20Semester%20Examination,%202024&semester=III&session=2024&regNo={REG}&exam_held=July%2F2025", true));
            repository.save(new ResultLink("2nd Sem (23-27)", "https://results.beup.ac.in/ResultsBTech2ndSem2024_B2023Pub.aspx?Sem=II&RegNo={REG}", true));
            repository.save(new ResultLink("1st Sem (23-27)", "https://results.beup.ac.in/ResultsBTech1stSem2023_B2023Pub.aspx?Sem=I&RegNo={REG}", true));

            // Batch 2024-28
            repository.save(new ResultLink("1st Sem (24-28)", "https://results.beup.ac.in/ResultsBTech1stSem2024_B2024Pub.aspx?Sem=I&RegNo={REG}", true));
        }
    }

    /**
     * Retrieves all ACTIVE links from the database formatted as a Map.
     * This keeps the return type compatible with your existing Ingestion Service.
     */
    public Map<String, String> getAllLinks() {
        List<ResultLink> links = repository.findByIsActiveTrue();

        // Convert List to LinkedHashMap to preserve DB insertion order
        return links.stream()
                .collect(Collectors.toMap(
                        ResultLink::getLinkKey,   // FIXED: Changed from getAttributeName
                        ResultLink::getUrlTemplate,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));
    }

    public String getUrl(String key) {
        return repository.findByLinkKey(key)
                .map(ResultLink::getUrlTemplate)
                .orElse(null);
    }
}