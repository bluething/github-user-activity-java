package io.github.bluething.playground.java;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

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
}