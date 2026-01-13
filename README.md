📊 LogAnalyzer

A high-performance Log Analysis system built with Spring Boot and Elasticsearch. This application allows users to ingest, store, search, and analyze application logs efficiently using a distributed search engine.

This project covers the full development lifecycle from use-case definition to Docker deployment and API testing.

🚀 Milestones & Features

This project was built following a structured 6-milestone roadmap:
1. Project Description

LogAnalyzer serves as a centralized log management service. Instead of grepping through massive text files, this service indexes logs into Elasticsearch, allowing for millisecond-latency searches and complex filtering.
2. Use Cases

The system supports the following core operations:

    - Ingest Logs: Send individual log entries via REST API.

    - Bulk Ingestion: Upload large log files (.txt) for automatic parsing and indexing.

    - Search: Full-text search on log messages (e.g., "database timeout").

    - Filtering: Filter by Log Level (INFO, WARN, ERROR) or Date Range.

    - Quick Views: Retrieve the latest 10 logs or all logs from the last 24 hours.

    - Maintenance: Delete log entries by ID.

3. REST API & Swagger Documentation

The project includes fully integrated API documentation using springdoc-openapi.

    Interactive UI: Access Swagger UI at http://localhost:8080/swagger-ui.html

    Spec: OpenAPI v3 specification available at /v3/api-docs.

4. Elasticsearch Mapping

Data is structured using strict typing to ensure efficient querying:

    Timestamp: Stored as Date (ISO-8601) for range queries.

    Level & Service: Stored as Keyword for exact matching and aggregations.

    Message: Stored as Text for full-text search tokenization.

5. Implementation (Tech Stack)

   Language: Java 21

   Framework: Spring Boot 3.3+

   Database: Elasticsearch 9.2.2 (Running in Docker)

   Containerization: Docker & Docker Compose

   Build Tool: Maven


6. Testing (Postman & Scripts)

   Postman: A complete collection is available to test all endpoints.

   Automation: Includes a Python script to generate thousands of realistic dummy logs for load testing.