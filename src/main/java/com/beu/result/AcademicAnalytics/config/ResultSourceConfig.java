package com.beu.result.AcademicAnalytics.config;

import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration Registry for External Data Sources.
 * <p>
 * This component acts as a centralized dictionary for resolving academic result portal endpoints.
 * It maps human-readable semester identifiers to their corresponding external API or DOM endpoints.
 * </p>
 */
@Component
public class ResultSourceConfig {

    /**
     * Registry storage.
     * Key: Display Name (e.g., "5th Sem 2025")
     * Value: Endpoint URL Template with dynamic placeholder {REG}
     */
    private final Map<String, String> endpointRegistry = new LinkedHashMap<>();

    public ResultSourceConfig() {
        initializeRegistry();
    }

    /**
     * Populates the registry with available external data sources.
     * <p>
     * NOTE: The placeholder "{REG}" is a reserved token that will be dynamically
     * replaced by the Ingestion Service during runtime.
     * </p>
     */
    private void initializeRegistry() {

        // --- ARCHIVED / LEGACY BATCHES (2021-2025) ---
        /*
        endpointRegistry.put("8th Sem 2025 (Batch 2021-25)", "https://results.beup.ac.in/ResultsBTech8thSem2025Pub.aspx?Sem=VIII&RegNo={REG}");
        endpointRegistry.put("7th Sem (Batch 2021-25)", "https://results.beup.ac.in/ResultsBTech7thSem2024_B2021Pub.aspx?Sem=VII&RegNo={REG}");
        endpointRegistry.put("6th Sem (Batch 2021-25)", "https://results.beup.ac.in/ResultsBTech6thSem2024_B2021Pub.aspx?Sem=VI&RegNo={REG}");
        endpointRegistry.put("5th Sem (Batch 2021-25)", "https://results.beup.ac.in/ResultsBTech5thSem2023_B2021Pub.aspx?Sem=V&RegNo={REG}");
        endpointRegistry.put("4th Sem (Batch 2021-25)", "https://results.beup.ac.in/ResultsBTech4thSem2023_B2021Pub.aspx?Sem=IV&RegNo={REG}");
        endpointRegistry.put("3rd Sem (Batch 2021-25)", "https://results.beup.ac.in/ResultsBTech3rdSem2022_B2021Pub.aspx?Sem=III&RegNo={REG}");
        */

        // --- ACTIVE BATCH (2022-2026) ---
        endpointRegistry.put("5th Sem 2025 (Batch 2022-26)",
                "https://results.beup.ac.in/ResultsBTech5thSem2024_B2022Pub.aspx?Sem=V&RegNo={REG}");

        endpointRegistry.put("4th Sem (Batch 2022-26)",
                "https://results.beup.ac.in/ResultsBTech4thSem2024_B2022Pub.aspx?Sem=IV&RegNo={REG}");

        endpointRegistry.put("3rd Sem (Batch 2022-26)",
                "https://results.beup.ac.in/ResultsBTech3rdSem2023_B2022Pub.aspx?Sem=III&RegNo={REG}");

        endpointRegistry.put("2nd Sem (Batch 2022-26)",
                "https://results.beup.ac.in/ResultsBTech2ndSem2023_B2022Pub.aspx?Sem=II&RegNo={REG}");

        endpointRegistry.put("1st Sem (Batch 2022-26)",
                "https://results.beup.ac.in/ResultsBTech1stSem2022_B2022Pub.aspx?Sem=I&RegNo={REG}");


        // --- ACTIVE BATCH (2023-2027) ---
        endpointRegistry.put("3rd Sem 2025 (Batch 2023-27)",
                "https://beu-bih.ac.in/result-three?name=B.Tech.%203rd%20Semester%20Examination,%202024&semester=III&session=2024&regNo={REG}&exam_held=July%2F2025");

        endpointRegistry.put("2nd Sem (Batch 2023-27)",
                "https://results.beup.ac.in/ResultsBTech2ndSem2024_B2023Pub.aspx?Sem=II&RegNo={REG}");

        endpointRegistry.put("1st Sem (Batch 2023-27)",
                "https://results.beup.ac.in/ResultsBTech1stSem2023_B2023Pub.aspx?Sem=I&RegNo={REG}");


        // --- INCOMING BATCH (2024-2028) ---
        endpointRegistry.put("1st Sem 2025 (Batch 2024-28)",
                "https://results.beup.ac.in/ResultsBTech1stSem2024_B2024Pub.aspx?Sem=I&RegNo={REG}");
    }

    /**
     * Retrieves the full map of available data sources.
     * @return Map containing Display Name -> URL Template
     */
    public Map<String, String> getAllLinks() {
        return endpointRegistry;
    }

    /**
     * Resolves a specific endpoint template by its display key.
     * @param key The identifier string (e.g. "5th Sem 2025...")
     * @return The URL template string
     */
    public String getUrl(String key) {
        return endpointRegistry.get(key);
    }
}