package org.botog.loganalyzer;

import java.util.Map;

public record LogStats(
        long total,
        Map<String, Long> byLevel,
        Map<String, Long> byService,
        double errorRate
) {}