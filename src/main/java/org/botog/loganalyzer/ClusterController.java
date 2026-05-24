package org.botog.loganalyzer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/cluster")
public class ClusterController {

    private final ElasticsearchClient elasticsearchClient;

    public ClusterController(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @GetMapping("/health")
    public HealthResponse health() throws IOException {
        return elasticsearchClient.cluster().health(h -> h);
    }
}