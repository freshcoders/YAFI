package nl.freshcoders.fit.tracing;


import nl.freshcoders.fit.tracing.span.Span;
import nl.freshcoders.fit.tracing.span.Trace;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

public class ElasticSource {

    public RestHighLevelClient client;

    private Instant start;

    private final String PATTERN_FORMAT = "yyyy-MM-dd";

    public ElasticSource(String host, Integer port) {

        Instant now = Instant.now();

        start = now.minus(5, ChronoUnit.MINUTES);

        RestClientBuilder restClientBuilder =
                RestClient.builder(new HttpHost(host, port));
        RestClient rc = restClientBuilder.build();

        // local setup does not require creds
        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials("", "");

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, credentials);

        restClientBuilder.setHttpClientConfigCallback(
                httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));

        client = new RestHighLevelClient(restClientBuilder);
    }


    private String formatSpanDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT)
                .withZone(ZoneId.systemDefault());
        String formattedInstant = formatter.format(start);

        return formattedInstant;
    }

    public Set<String> getTraceList() {
        Set<String> traces = new HashSet<>();
        try {
            SearchSourceBuilder builder = new SearchSourceBuilder()
                    .query(QueryBuilders.wildcardQuery("traceID", "*"))
                    .query(QueryBuilders.rangeQuery("startTimeMillis").gt(start.toEpochMilli()))
                    .aggregation(AggregationBuilders.terms("traceID").field("traceID"))
                    .size(10000);
            // TODO, implement scroll, better querying
            SearchRequest searchRequest = new SearchRequest();



            searchRequest.indices("aaa-bbb-" + formatSpanDate());
            searchRequest.searchType(SearchType.DFS_QUERY_THEN_FETCH);
            searchRequest.source(builder);
            SearchResponse response = client
                    .search(
                            searchRequest,
                            RequestOptions.DEFAULT
                    );

            response.getHits().forEach(t -> traces.add((String) t.getSourceAsMap().get("traceID")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return traces;
    }


    public Trace buildTrace(String traceId) {
        SearchSourceBuilder builder = new SearchSourceBuilder()
                .postFilter(QueryBuilders.fuzzyQuery("traceID", traceId));

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("aaa-bbb-" + formatSpanDate());
        searchRequest.searchType(SearchType.DFS_QUERY_THEN_FETCH);
        searchRequest.source(builder);

        try {
            SearchResponse response = client
                    .search(
                            searchRequest,
                            RequestOptions.DEFAULT
                    );

            Trace trace = new Trace();
            response.getHits().forEach(
                    x -> trace.addSpan(Span.fromElasticMap(x.getSourceAsMap()))
            );

            trace.resolveChildRelations();
            return trace;


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
