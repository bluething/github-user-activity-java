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

### Why using ZonedDateTime?
I picked ZonedDateTime for the timestamp because the GitHub API gives me an ISO-8601 string with an offset (e.g. 2025-04-25T03:14:15Z or +02:00). By using ZonedDateTime I can:
1. Preserve the original offset/timezone exactly as sent by GitHub.
2. Parse/format straight with DateTimeFormatter.ISO_OFFSET_DATE_TIME.
3. Have full time-zone support if later want to convert it into another zone (e.g. the user’s local time).

If you only care about an instant in time and don’t need zone conversions, Instant would also work. But ZonedDateTime gives you the most flexibility when handling offsets and human-readable fields.

---
### Why using Caffeine?
Caffeine is a go-to choice for an in-heap Java cache because it gives you best-in-class performance and a very rich feature set, yet remains extremely easy to plug in:
* Blazing-fast, low-latency  
  Under the covers Caffeine uses algorithms like W-TinyLFU and segmented LRU to deliver consistently low look-ups and eviction overhead—even at high concurrency.
* Advanced eviction policies  
You get both expireAfterWrite and expireAfterAccess, plus size-based (maximum entries or maximum weight) eviction and write-through or async refresh support.
* Thread-safe without contention  
Caffeine’s highly optimized internal sharding and lock-elision mean you don’t pay heavy synchronization costs even under heavy multi-threaded access.
* Minimal footprint  
Compared to heavyweight caches (EHCache, Infinispan), Caffeine has very little configuration surface and no external dependencies beyond its own JAR.
* Modern API  
Its builder-style, callback hooks (write, eviction listeners), and optional async loading integrate cleanly with Java 8+ lambdas and CompletionStage.

#### Alternatives & Why Caffeine Wins

* Guava Cache: solid, but Caffeine is its successor—same API style with markedly better hit rates and throughput.
* External caches (Redis, Memcached): great for shared/distributed caching, but for simple per-instance needs you avoid serialization overhead and network hops by staying in-process.
* JCache (JSR-107): standard API, but implementations vary in performance and features; Caffeine offers a purpose-built, battle-tested library.