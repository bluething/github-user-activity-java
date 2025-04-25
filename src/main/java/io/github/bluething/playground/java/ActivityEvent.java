package io.github.bluething.playground.java;

import java.time.ZonedDateTime;

record ActivityEvent(ZonedDateTime timestamp,
                     String type,
                     String repo,
                     String details) {
}
