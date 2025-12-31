package com.beu.result.beup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "beu.result")
public class DownloaderConfig {

    /**
     * Optional base URL (used only if you want a default)
     * Example:
     * https://results.beup.ac.in
     */
    private String baseUrl = "https://results.beup.ac.in";

    /**
     * Default output directory (can be overridden from UI)
     */
    private String outputDir = "beu-results";

    /**
     * Optional registration range defaults
     */
    private long startReg;
    private long endReg;

    /* ===================== GETTERS & SETTERS ===================== */

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public long getStartReg() {
        return startReg;
    }

    public void setStartReg(long startReg) {
        this.startReg = startReg;
    }

    public long getEndReg() {
        return endReg;
    }

    public void setEndReg(long endReg) {
        this.endReg = endReg;
    }
}
