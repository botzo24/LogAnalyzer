package org.botog.loganalyzer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.time.Instant;

public interface LogRepository extends ElasticsearchRepository<LogEntry, String> {
    Page<LogEntry> findByLevel(String level, Pageable pageable);
    Page<LogEntry> findByMessageContaining(String keyword, Pageable pageable);
    Page<LogEntry> findByTimestampBetween(Instant start, Instant end, Pageable pageable);
}