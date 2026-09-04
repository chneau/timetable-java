# timetable-java

A high-performance, capacity-constrained timeline and scheduling engine for Java 26+.

[![Java 26](https://img.shields.io/badge/Java-26-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## ⚡ Features & Performance

- **Capacity-Constrained Scheduling**: Accumulates intervals and guarantees total simultaneous capacity never exceeds a configured threshold (`max`).
- **OpenHours Integration**: Seamlessly integrates with [openhours-java](https://github.com/chneau/openhours-java) via `Whenable` constraints (operating hours, shifts, maintenance windows).
- **Sub-Microsecond Operations**: High-throughput `add` and `when` queries (~1–4 µs per operation) with clean immutable state transitions.
- **Java 26 Modern Architecture**: Utilizes immutable Java `record Point`, `List.copyOf`, tree-map simplification, and modern language features.
- **Built-in Jackson Support**: Built-in Jackson serializers and deserializers (`@JsonSerialize` / `@JsonDeserialize`).

---

## 🧠 Architecture & Design

`TimeTable` represents step functions of resource usage over time:
1. **Delta Event Encoding**: Each allocation of duration $D$ and capacity $C$ at time $T$ creates delta points: $+C$ at $T$, and $-C$ at $T + D$.
2. **Deterministic Simplification**: Overlapping deltas occurring at identical points in time are summed, and zero-sum transitions are eliminated.
3. **Capacity Invariant Verification**: The timeline verifies that prefix sums at every point in time remain $\le \text{max}$ and close to zero net change at infinity.
4. **Constraint Forwarding**: Querying `when(start, duration, capacity)` evaluates availability under external operational constraints (such as `OpenHours`) and finds the earliest valid start time.

---

## 🚀 Quick Start

### Installation

#### Option 1: Via JitPack (Zero authentication required)

##### Gradle (Groovy)

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.chneau:timetable-java:v1.0.0'
}
```

##### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.chneau:timetable-java:v1.0.0")
}
```

##### Maven (`pom.xml`)

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.chneau</groupId>
        <artifactId>timetable-java</artifactId>
        <version>v1.0.0</version>
    </dependency>
</dependencies>
```

---

#### Option 2: Via GitHub Packages (`maven.pkg.github.com`)

##### Gradle (Groovy)

```groovy
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/chneau/timetable-java")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation 'chneau:timetable:1.0.0'
}
```

---

### Usage Example

```java
import chneau.openhours.OpenHours;
import chneau.timetable.TimeTable;

import java.time.Duration;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        // 1. Constrain timetable with maximum capacity 2 and business hours
        var businessHours = OpenHours.parse("Mo-Fr 09:00-17:00");
        var tt = new TimeTable(2.0, businessHours);

        var start = LocalDateTime.of(2026, 9, 7, 9, 0); // Monday 9:00 AM

        // 2. Book 1 capacity unit for 2 hours
        tt = tt.add(start, Duration.ofHours(2), 1.0);

        // 3. Find the next available time slot for 2 capacity units lasting 3 hours
        LocalDateTime nextSlot = tt.when(start, Duration.ofHours(3), 2.0);
        System.out.println("Earliest start time: " + nextSlot);

        // 4. Inspect current points and capacity
        System.out.println("Points count: " + tt.size());
        System.out.println("Max capacity: " + tt.getMax());
    }
}
```

---

## 📊 Benchmarks

Run benchmarks using the Gradle suite:

```bash
./gradlew bench -q
```

Typical performance on Java 26:

| Benchmark Task | Operations | Throughput / Latency |
| :--- | :--- | :--- |
| **Sequential Add** | 50,000 ops | **~4.1 µs / op** |
| **Capacity Search (`when`)** | 20,000 ops | **~1.2 µs / op** |
| **Jackson Deserialization** | 10,000 ops | **~27.9 µs / op** |

---

## 🛠️ Testing & Verification

Run the test suite with JUnit 5:

```bash
./gradlew test
```

## 📄 License

This project is licensed under the [MIT License](LICENSE).
