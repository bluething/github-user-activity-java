package io.github.bluething.playground.java;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public interface GithubActivity {
    JsonNode fetchUser(String username) throws IOException, InterruptedException;
    JsonNode fetchEvents(String username) throws IOException, InterruptedException;
}
