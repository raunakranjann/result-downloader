##  Academic Analytics & Archival System


![Java](https://img.shields.io/badge/Java-17-orange) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.0-green) ![Playwright](https://img.shields.io/badge/Automation-Playwright-blue) ![Status](https://img.shields.io/badge/Status-Stable-brightgreen)

###  Project Overview
This project is an enterprise-grade solution designed to modernize how academic institutions handle student performance data. It replaces manual result aggregation with an automated **ETL (Ingestion) Pipeline**, provides a **Business Intelligence Dashboard** for real-time insights, and features a **Queue-Based Digital Archival Subsystem** for bulk PDF transcript generation.


###  Key Features
* **Automated Ingestion Engine:** Uses Microsoft Playwright (Headless Chromium) to scrape and normalize unstructured result data from university portals.
* **Business Intelligence (BI) Dashboard:** Visualizes KPIs like Branch-wise Average CGPA, Pass/Fail Ratios, and Grade Distribution Curves using Chart.js.
* **Digital Archival System:** A robust queue management system that processes multiple colleges/branches sequentially to generate and merge thousands of PDF transcripts.
* **Enterprise Architecture:** Implements Asynchronous processing (`@Async`), atomic telemetry for live progress tracking, and fault-tolerant retry logic.



###  Tech Stack
* **Backend:** Java Spring Boot (Web, Data JPA)
* **Automation:** Microsoft Playwright
* **Database:** PostgreSQL
* **Frontend:** Thymeleaf, Bootstrap 5, JavaScript (Fetch API)
* **Tools:** Apache PDFBox, Maven

<hr>
