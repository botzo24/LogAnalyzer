package org.botog.loganalyzer;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogRepository repository;

    public LogController(LogRepository repository) {
        this.repository = repository;
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
