package org.botog.loganalyzer;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogRepository repository;
    private final ElasticsearchOperations operations;

    public LogController(LogRepository repository, ElasticsearchOperations operations) {
        this.repository = repository;
        this.operations = operations;
    }

    @PostMapping(consumes = "application/json")
    public LogEntry addLog(@RequestBody LogEntry log) {
        log.setTimestamp(java.time.Instant.now());
        return repository.save(log);
    }

    @GetMapping("/search")
    public List<LogEntry> search(@RequestParam String keyword) {
        return repository.findByMessageContaining(keyword, PageRequest.of(0, 500)).getContent();
    }

    @GetMapping("/level/{level}")
    public List<LogEntry> byLevel(@PathVariable String level) {
        return repository.findByLevel(level, PageRequest.of(0, 500)).getContent();
    }

    @GetMapping("/date")
    public List<LogEntry> byDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        return repository.findByTimestampBetween(start, end, PageRequest.of(0, 1000)).getContent();
    }

    @GetMapping("/recent")
    public List<LogEntry> recent() {
        Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        return repository.findByTimestampBetween(twentyFourHoursAgo, Instant.now(), PageRequest.of(0, 1000)).getContent();
    }

    @GetMapping("/latest")
    public List<LogEntry> latest() {
        return repository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "timestamp")))
                .getContent();
    }

    @GetMapping("/stats")
    public LogStats stats() {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.matchAll(m -> m))
                .withAggregation("by_level",
                        Aggregation.of(a -> a.terms(t -> t.field("level").size(50))))
                .withAggregation("by_service",
                        Aggregation.of(a -> a.terms(t -> t.field("service").size(200))))
                .withMaxResults(0)
                .build();

        SearchHits<LogEntry> hits = operations.search(query, LogEntry.class);
        ElasticsearchAggregations aggregations = (ElasticsearchAggregations) hits.getAggregations();

        Map<String, Long> byLevel = bucketsToMap(aggregations, "by_level");
        Map<String, Long> byService = bucketsToMap(aggregations, "by_service");

        long total = hits.getTotalHits();
        long errors = byLevel.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase("ERROR"))
                .mapToLong(Map.Entry::getValue)
                .sum();
        double errorRate = total == 0 ? 0.0 : (double) errors / total;

        return new LogStats(total, byLevel, byService, errorRate);
    }

    private Map<String, Long> bucketsToMap(ElasticsearchAggregations aggregations, String name) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (aggregations == null) {
            return result;
        }
        ElasticsearchAggregation aggregation = aggregations.get(name);
        if (aggregation == null) {
            return result;
        }
        for (StringTermsBucket bucket : aggregation.aggregation().getAggregate().sterms().buckets().array()) {
            result.put(bucket.key().stringValue(), bucket.docCount());
        }
        return result;
    }

    @DeleteMapping("/{id}")
    public void deleteLog(@PathVariable String id) {
        repository.deleteById(id);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadLogFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "File is empty";
        }

        List<LogEntry> logs = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                LogEntry entry = parseLine(line);
                if (entry != null) {
                    logs.add(entry);
                }
            }

            repository.saveAll(logs);
            return "Successfully indexed " + logs.size() + " logs.";

        } catch (Exception e) {
            return "Error processing file: " + e.getMessage();
        }
    }

    private LogEntry parseLine(String line) {
        try {
            String[] parts = line.split(" ", 3);

            if (parts.length < 3)
                return null;

            LogEntry log = new LogEntry();
            log.setLevel(parts[0].trim());
            log.setService(parts[1].trim());
            log.setMessage(parts[2].trim());

            return log;
        } catch (Exception e) {
            return null;
        }
    }
}
