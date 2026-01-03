package com.beu.result.DatabaseIntegration.config;

import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ResultLinkConfig {

    private final Map<String, String> links = new LinkedHashMap<>();

    public ResultLinkConfig() {
        // Add your links here. The Key is what shows in the Dropdown.
        // The Value is the URL pattern with {REG}


        links.put("8th Sem 2025 (Batch 2021-25)",
                "https://results.beup.ac.in/ResultsBTech8thSem2025Pub.aspx?Sem=VIII&RegNo={REG}");

        links.put("7th Sem (Batch 2021-25)","https://results.beup.ac.in/ResultsBTech7thSem2024_B2021Pub.aspx?Sem=VII&RegNo={REG}");

        links.put("6th Sem (Batch 2021-25)","https://results.beup.ac.in/ResultsBTech6thSem2024_B2021Pub.aspx?Sem=VI&RegNo={REG}");

        links.put("5th Sem (Batch 2021-25)","https://results.beup.ac.in/ResultsBTech5thSem2023_B2021Pub.aspx?Sem=V&RegNo={REG}");

        links.put("4th Sem (Batch 2021-25)","https://results.beup.ac.in/ResultsBTech4thSem2023_B2021Pub.aspx?Sem=IV&RegNo={REG}");

        links.put("3rd Sem (Batch 2021-25)","https://results.beup.ac.in/ResultsBTech3rdSem2022_B2021Pub.aspx?Sem=III&RegNo={REG}");





        links.put("5th Sem 2025 (Batch 2022-26)",
                "https://results.beup.ac.in/ResultsBTech5thSem2024_B2022Pub.aspx?Sem=V&RegNo={REG}");

        links.put("4th Sem (Batch 2022-26)","https://results.beup.ac.in/ResultsBTech4thSem2024_B2022Pub.aspx?Sem=IV&RegNo={REG}");

        links.put("3rd Sem (Batch 2022-26)","https://results.beup.ac.in/ResultsBTech3rdSem2023_B2022Pub.aspx?Sem=III&RegNo={REG}");

        links.put("2nd Sem (Batch 2022-26)","https://results.beup.ac.in/ResultsBTech2ndSem2023_B2022Pub.aspx?Sem=II&RegNo={REG}");

        links.put("1st Sem (Batch 2022-26)", "https://results.beup.ac.in/ResultsBTech1stSem2022_B2022Pub.aspx?Sem=I&RegNo={REG}");






        links.put("3rd Sem 2025 (Batch 2023-27)",
                "https://beu-bih.ac.in/result-three?name=B.Tech.%203rd%20Semester%20Examination,%202024&semester=III&session=2024&regNo={REG}&exam_held=July%2F2025");

        links.put("2nd Sem (Batch 2023-27)","https://results.beup.ac.in/ResultsBTech2ndSem2024_B2023Pub.aspx?Sem=II&RegNo={REG}");

        links.put("1st Sem NEW (Batch 2023-27)","https://results.beup.ac.in/ResultsBTech1stSem2024_old_B2023Pub.aspx?Sem=I&RegNo={REG}");

        links.put("1st Sem OLD (Batch 2023-27)", "https://results.beup.ac.in/ResultsBTech1stSem2023_B2023Pub.aspx?Sem=I&RegNo={REG}");






        links.put("1st Sem 2025 (Batch 2024-28)",
                "https://results.beup.ac.in/ResultsBTech1stSem2024_B2024Pub.aspx?Sem=I&RegNo={REG}");


        // You can add more easily here in the future
        // links.put("Display Name", "URL Pattern");
    }

    public Map<String, String> getAllLinks() {
        return links;
    }

    public String getUrl(String key) {
        return links.get(key);
    }
}