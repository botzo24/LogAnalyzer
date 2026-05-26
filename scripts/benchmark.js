// k6 load benchmark for LogAnalyzer.
//
// Measures throughput and p50/p95/p99 latency of the REST endpoints against a
// running app + Elasticsearch cluster.
//
// Prerequisites:
//   - Install k6:  https://grafana.com/docs/k6/latest/set-up/install-k6/
//   - App reachable at BASE_URL (default http://localhost:8080) with ES up.
//
// Usage (run from the scripts/ directory so the sample file resolves):
//   cd scripts
//   k6 run benchmark.js                       # read-only query mix (default)
//   k6 run -e SEED=true benchmark.js          # upload sample logs once, then query
//   k6 run -e MODE=ingest benchmark.js        # bulk-upload throughput only
//   k6 run -e MODE=all -e SEED=true benchmark.js   # queries + ingest together
//   k6 run -e BASE_URL=http://host:8080 benchmark.js
//
// MODE  : queries | ingest | all   (default: queries)
// SEED  : true to upload large_test_logs.txt once in setup() before the test
// BASE_URL : target base URL (default http://localhost:8080)

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const MODE = __ENV.MODE || 'queries';

// Loaded once at init time; reused for every upload request.
const logFile = open('./large_test_logs.txt', 'b');

// Keywords/levels that actually occur in large_test_logs.txt so queries hit data.
const KEYWORDS = ['login', 'credentials', 'Disk', 'percent', 'timeout', 'failed', 'memory'];
const LEVELS = ['INFO', 'WARN', 'ERROR'];

// Per-endpoint latency trends (true => report in ms with percentiles).
const latSearch = new Trend('lat_search', true);
const latLevel = new Trend('lat_level', true);
const latStats = new Trend('lat_stats', true);
const latRecent = new Trend('lat_recent', true);
const latUpload = new Trend('lat_upload', true);
const errors = new Rate('errors');

function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function buildScenarios() {
  const s = {};
  if (MODE === 'queries' || MODE === 'all') {
    s.queries = {
      executor: 'ramping-vus',
      exec: 'queryMix',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 20 }, // ramp up
        { duration: '30s', target: 20 }, // sustain
        { duration: '10s', target: 0 },  // ramp down
      ],
    };
  }
  if (MODE === 'ingest' || MODE === 'all') {
    s.ingest = {
      executor: 'constant-vus',
      exec: 'ingest',
      vus: 2,
      duration: '30s',
    };
  }
  return s;
}

export const options = {
  scenarios: buildScenarios(),
  thresholds: {
    http_req_failed: ['rate<0.01'], // <1% failed requests
    errors: ['rate<0.01'],
    lat_search: ['p(95)<800'],
    lat_stats: ['p(95)<1000'],
  },
};

// Optional one-time data seed so query runs have something to search.
export function setup() {
  if (__ENV.SEED === 'true') {
    const res = http.post(`${BASE_URL}/api/logs/upload`, {
      file: http.file(logFile, 'large_test_logs.txt', 'text/plain'),
    });
    console.log(`seed upload -> status=${res.status} body=${res.body}`);
  }
}

// Weighted read mix across the query endpoints.
export function queryMix() {
  const r = Math.random();
  let res;
  if (r < 0.4) {
    res = http.get(`${BASE_URL}/api/logs/search?keyword=${pick(KEYWORDS)}`);
    latSearch.add(res.timings.duration);
  } else if (r < 0.65) {
    res = http.get(`${BASE_URL}/api/logs/level/${pick(LEVELS)}`);
    latLevel.add(res.timings.duration);
  } else if (r < 0.85) {
    res = http.get(`${BASE_URL}/api/logs/stats`);
    latStats.add(res.timings.duration);
  } else {
    res = http.get(`${BASE_URL}/api/logs/recent`);
    latRecent.add(res.timings.duration);
  }
  errors.add(!check(res, { 'status is 200': (x) => x.status === 200 }));
}

// Bulk-ingest throughput: re-upload the full sample file each iteration.
export function ingest() {
  const res = http.post(`${BASE_URL}/api/logs/upload`, {
    file: http.file(logFile, 'large_test_logs.txt', 'text/plain'),
  });
  latUpload.add(res.timings.duration);
  errors.add(!check(res, {
    'status is 200': (x) => x.status === 200,
    'logs indexed': (x) => String(x.body).includes('indexed'),
  }));
}