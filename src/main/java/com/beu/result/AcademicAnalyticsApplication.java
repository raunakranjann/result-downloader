package com.beu.result;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import jakarta.annotation.PostConstruct; // Use javax.annotation.PostConstruct if using older Spring
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

@EnableAsync
@SpringBootApplication
public class AcademicAnalyticsApplication {

    public static void main(String[] args) {
        // 1. Check if the port is free
        try (ServerSocket socket = new ServerSocket(2006)) {
            // If we get here, the port was free.
            // The 'try-with-resources' block will now CLOSE the socket automatically.
        } catch (IOException e) {
            // If we get here, the port is truly blocked by another instance
            System.err.println("Academic Analytics is already running.");
            System.exit(0);
        }

        // 2. Now that the socket is CLOSED, Spring can safely take the port
        SpringApplication.run(AcademicAnalyticsApplication.class, args);
    }

    // 2. Folder Creation: Ensures the DB/Log folder exists in User Home
    @PostConstruct
    public void init() {
        String path = System.getProperty("user.home") + File.separator + "AcademicAnalytics";
        File folder = new File(path);
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (created) {
                System.out.println("Created application data directory at: " + path);
            }
        }
    }
}