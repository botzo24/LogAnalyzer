package org.botog.loganalyzer;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.time.Instant;
import java.util.List;

public interface LogRepository extends ElasticsearchRepository<LogEntry, String> {
    List<LogEntry> findByLevel(String level);
    List<LogEntry> findByMessageContaining(String keyword);
    List<LogEntry> findByTimestampBetween(Instant start, Instant end);
}