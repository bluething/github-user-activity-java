# GitHub Activity CLI

A simple Java command‑line application to fetch and display a GitHub user’s recent public activity, with optional filtering, structured output, caching, and authentication support.
Part of [Project Ideas](https://roadmap.sh/projects/github-user-activity)

---

### Features
* Fetch Public Events: Uses GitHub’s /users/:username/events endpoint
* Event Filtering: Pass --type=PushEvent,IssuesEvent to show only certain event types
* Structured Table: Outputs a clean table with Timestamp, Type, Repository, and Details
* In‑Memory Caching: Caches API responses for 5 minutes using Caffeine
* User Summary: Displays name, company, location, public repos, followers, and following
* Authentication (Optional): Honor GITHUB_TOKEN env var for higher rate limits and private data
* Production‑Level HTTPClient: Configurable timeouts, HTTP/2, and virtual‑thread executor support (Java21+)

---

### Requirements
* Java21+
* Maven3.6+
* Internet connection for GitHub API calls

---

### Installation
```text
# Clone
git clone https://github.com/your‑org/github‑activity‑cli.git
cd github‑activity‑cli

# Build
mvn clean package
```

---

### Usage

```text
# Basic fetch
java -jar target/github-activity-cli-1.1.0.jar <username>

# Filter by event type(s)
java -jar target/github-activity-cli-1.1.0.jar <username> --type=PushEvent,PullRequestEvent
```

---

Examples

```text
User: Habib Ali Machpud
Company: -, Location: Jakarta
Repos: 68, Followers: 3, Following: 4

Timestamp             Type             Repository                      Details
--------------------------------------------------------------------------------
2025-04-22 16:36      PushEvent        bluething/task-tracker-java     Pushed 4 commits
2025-04-18 16:38      PushEvent        bluething/provisioning-hw       Pushed 1 commit
2025-04-18 16:16      PushEvent        bluething/provisioning-hw       Pushed 1 commit
2025-04-18 16:11      PushEvent        bluething/provisioning-hw       Pushed 4 commits
2025-04-18 15:07      PushEvent        bluething/provisioning-hw       Pushed 1 commit
2025-04-18 14:56      PushEvent        bluething/provisioning-hw       Pushed 4 commits
2025-04-18 09:10      PushEvent        bluething/provisioning-hw       Pushed 4 commits
2025-04-17 19:02      PushEvent        bluething/provisioning-hw       Pushed 4 commits
2025-04-17 18:08      PushEvent        bluething/provisioning-hw       Pushed 7 commits
```

---

### Testing
Unit tests use JUnit5 and Mockito to mock the HttpClient—no real GitHub calls.