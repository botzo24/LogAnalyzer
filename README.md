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

    - Statistics: Summarize indexed logs via `GET /api/logs/stats` — total count, breakdown by level and service, and the overall error rate (computed with Elasticsearch terms aggregations).

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

   Benchmarks: `scripts/benchmark.js` is a [k6](https://grafana.com/docs/k6/latest/) load test reporting throughput and p50/p95/p99 latency per endpoint. See `scripts/BENCHMARKS.md` for the full step-by-step commands. Quick start (live app + ES required):

   ```bash
   cd scripts
   k6 run -e SEED=true benchmark.js        # seed sample logs, then run the query mix
   k6 run -e MODE=ingest benchmark.js      # bulk-upload throughput only
   k6 run -e MODE=all -e SEED=true benchmark.js
   ```

   Env vars: `MODE` (`queries`|`ingest`|`all`, default `queries`), `SEED` (`true` to upload `large_test_logs.txt` once first), `BASE_URL` (default `http://localhost:8080`).