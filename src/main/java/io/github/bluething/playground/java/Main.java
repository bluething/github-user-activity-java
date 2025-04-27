package io.github.bluething.playground.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            // use HTTP/2, so I get connection multiplexing and header compression
            .version(HttpClient.Version.HTTP_2)
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .build();
    private static final Cache<String, JsonNode> cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length == 0) {
            System.err.println("Usage: github-activity <username> [--type=EventType1,EventType2,...]");
            System.exit(1);
        }

        String username = null;
        Set<String> filters = new HashSet<>();
        for (String arg : args) {
            if (arg.startsWith("--type=")) {
                filters.addAll(Arrays.asList(arg.substring(7).split(",")));
            } else if (username == null) {
                username = arg;
            }
        }

        if (username == null) {
            System.err.println("Username is required.");
            System.exit(1);
        }

        GithubActivity githubActivity = new GithubActivityService(httpClient, cache);
        printUserSummary(githubActivity.fetchUser(username));
        List<ActivityEvent> events = fetchEvents(githubActivity.fetchEvents(username), filters);
        displayTable(events);

    }

    private static void printUserSummary(JsonNode userJson) {
        System.out.printf("User: %s\n", userJson.path("name").asText("N/A"));
        System.out.printf("Company: %s, Location: %s\n",
                userJson.path("company").asText("-"),
                userJson.path("location").asText("-"));
        System.out.printf("Repos: %d, Followers: %d, Following: %d\n\n",
                userJson.path("public_repos").asInt(),
                userJson.path("followers").asInt(),
                userJson.path("following").asInt());
    }
    private static List<ActivityEvent> fetchEvents(JsonNode eventsJson, Set<String> filters) {
        if (!eventsJson.isArray()) return Collections.emptyList();

        List<ActivityEvent> list = new ArrayList<>();
        for (JsonNode node : eventsJson) {
            String type = node.path("type").asText();
            if (!filters.isEmpty() && !filters.contains(type)) continue;

            ZonedDateTime time = ZonedDateTime.parse(
                    node.path("created_at").asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String repo = node.path("repo").path("name").asText("-");
            String details = eventDetails(node, type);

            list.add(new ActivityEvent(time, type, repo, details));
        }
        return list;
    }
    private static String eventDetails(JsonNode node, String type) {
        JsonNode payload = node.path("payload");
        return switch (type) {
            case "PushEvent" -> {
                int count = payload.path("commits").size();
                yield String.format("Pushed %d commit%s", count, count>1?"s":"");
            }
            case "IssuesEvent" -> capitalize(payload.path("action").asText()) + " issue";
            case "IssueCommentEvent" -> "Commented on issue";
            case "WatchEvent" -> "Starred repo";
            default -> type;
        };
    }
    private static String capitalize(String s) {
        return (s == null || s.isEmpty()) ? s : s.substring(0,1).toUpperCase() + s.substring(1);
    }
    private static void displayTable(List<ActivityEvent> events) {
        if (events.isEmpty()) {
            System.out.println("No events to display.");
            return;
        }

        // Header
        System.out.printf("%-20s  %-15s  %-30s  %s%n",
                "Timestamp", "Type", "Repository", "Details");
        System.out.println("--------------------------------------------------------------------------------");

        // Rows
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (ActivityEvent e : events) {
            System.out.printf("%-20s  %-15s  %-30s  %s%n",
                    e.timestamp().format(fmt),
                    e.type(),
                    e.repo(),
                    e.details());
        }
    }
}