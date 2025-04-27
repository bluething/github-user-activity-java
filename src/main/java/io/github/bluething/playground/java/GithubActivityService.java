package io.github.bluething.playground.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

class GithubActivityService implements GithubActivity {
    private static final String EVENTS_URL   = "https://api.github.com/users/%s/events";
    private static final String USER_URL     = "https://api.github.com/users/%s";
    private static final Cache<String, JsonNode> cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    GithubActivityService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public JsonNode fetchUser(String username) throws IOException, InterruptedException {
        return fetchJson(String.format(USER_URL, username));
    }

    @Override
    public JsonNode fetchEvents(String username) throws IOException, InterruptedException {
        return cache.get(username + ":events", key -> {
            try {
                return fetchJson(String.format(EVENTS_URL, username));
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private JsonNode fetchJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github.v3+json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (200 != response.statusCode()) {
            throw new IOException("HTTP status code " + response.statusCode());
        }

        return objectMapper.readTree(response.body());
    }
}
