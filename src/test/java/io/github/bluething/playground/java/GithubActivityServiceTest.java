package io.github.bluething.playground.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GithubActivityServiceTest {
    @Mock HttpClient mockHttpClient;
    @Mock HttpResponse<String> mockHttpResponse;
    private AutoCloseable mock;

    private GithubActivityService githubActivityService;

    @BeforeEach
    void setup() {
        mock = MockitoAnnotations.openMocks(this);
        githubActivityService = new GithubActivityService(mockHttpClient);
    }
    @AfterEach
    void tearDown() throws Exception {
        mock.close();
    }

    @Test
    void fetchEvents_returnsParsedJson_withoutHittingNetwork() throws IOException, InterruptedException {
        String fakeJson = "[{\"type\":\"PushEvent\",\"created_at\":\"2025-04-25T10:15:30Z\",\"repo\":{\"name\":\"foo/bar\"},"
                + "\"payload\":{\"commits\":[{},{}]}}]";
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(fakeJson);
        // stub HttpClient.send(...) to always return our fake response
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(mockHttpResponse);

        JsonNode events = githubActivityService.fetchEvents("anyuser");
        assertTrue(events.isArray());
        assertEquals("PushEvent", events.get(0).get("type").asText());
    }
    @Test
    void fetchUser_handles404_gracefully() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(404);
        when(mockHttpResponse.body()).thenReturn("");
        doReturn(mockHttpResponse)
                .when(mockHttpClient)
                .send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));

        IOException ex = assertThrows(IOException.class, () -> githubActivityService.fetchUser("nope"));
        assertTrue(ex.getMessage().contains("404"));
    }
    @Test
    void fetchEvents_isCached_betweenCalls() throws IOException, InterruptedException {
        String fakeJson = "[{\"type\":\"PushEvent\",\"created_at\":\"2025-04-25T10:15:30Z\",\"repo\":{\"name\":\"foo/bar\"},"
                + "\"payload\":{\"commits\":[{}]}}]";
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(fakeJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        githubActivityService.fetchEvents("alice");
        githubActivityService.fetchEvents("alice");

        // assert: underlying HTTP send() was only called once
        verify(mockHttpClient, times(1))
                .send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
    @Test
    void fetchEvents_expiresCache_afterTtl() throws Exception {
        // build service with a cache that expires after 10ms
        GithubActivityService svc = new GithubActivityService(
                mockHttpClient,
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MILLISECONDS)
                        .build()
        );

        // stub response
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("[]");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        svc.fetchEvents("bob");           // cache miss → 1 HTTP call
        Thread.sleep(20);                 // wait past TTL
        svc.fetchEvents("bob");           // cache expired → 2nd HTTP call

        verify(mockHttpClient, times(2))
                .send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }


}