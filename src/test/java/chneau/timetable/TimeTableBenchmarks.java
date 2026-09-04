package chneau.timetable;

import chneau.openhours.OpenHours;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.PrintStream;
import java.time.Duration;
import java.time.LocalDateTime;

public class TimeTableBenchmarks {

    public static void main(String[] args) throws Exception {
        runBenchmarks(System.out);
    }

    public static void runBenchmarks(PrintStream out) throws Exception {
        out.println("========================================================");
        out.println("Running TimeTable Benchmarks (Java 26 Suite)");
        out.println("========================================================");

        var oh = OpenHours.parse("Mo-Fr 08:00-18:00");
        var baseTime = LocalDateTime.of(2026, 8, 24, 9, 0);
        var oneHour = Duration.ofHours(1);
        var mapper = new ObjectMapper();

        // Warm-up C2 JIT compiler
        var warmupTT = new TimeTable(5.0, oh);
        for (int i = 0; i < 5000; i++) {
            var next = warmupTT.add(baseTime.plusHours(i % 24), oneHour, 1.0);
            if (next != null) {
                warmupTT = next;
            }
            warmupTT.when(baseTime.plusHours(i % 10), oneHour, 1.0);
            if (warmupTT.size() > 50) {
                warmupTT = new TimeTable(5.0, oh);
            }
        }

        // Benchmark 1: Sequential Add Operations
        int addOps = 50_000;
        var tt = new TimeTable(100.0, oh);
        long t0 = System.nanoTime();
        for (int i = 0; i < addOps; i++) {
            var added = tt.add(baseTime.plusMinutes(i * 10), oneHour, 1.0);
            if (added != null) {
                tt = added;
            }
            if (tt.size() > 200) {
                tt = new TimeTable(100.0, oh);
            }
        }
        long t1 = System.nanoTime();
        double d1 = (t1 - t0) / 1_000_000.0;
        out.printf("1. Sequential Add (%d calls):               %6.1f ms (%.3f us/op)%n", addOps, d1, ((t1 - t0) / 1000.0) / addOps);

        // Benchmark 2: When (Capacity Query) with OpenHours constraint
        int whenOps = 20_000;
        var searchTT = new TimeTable(3.0, oh);
        for (int i = 0; i < 5; i++) {
            searchTT = searchTT.add(baseTime.plusHours(i * 2), Duration.ofHours(2), 2.0);
        }
        t0 = System.nanoTime();
        for (int i = 0; i < whenOps; i++) {
            searchTT.when(baseTime.plusHours(i % 40), oneHour, 2.0);
        }
        t1 = System.nanoTime();
        double d2 = (t1 - t0) / 1_000_000.0;
        out.printf("2. Capacity Search When (%d calls):        %6.1f ms (%.3f us/op)%n", whenOps, d2, ((t1 - t0) / 1000.0) / whenOps);

        // Benchmark 3: Jackson Serialization / Deserialization
        int jsonOps = 10_000;
        var sample = new TimeTable(10.0, oh);
        sample = sample.add(baseTime, oneHour, 1.0);
        sample = sample.add(baseTime.plusHours(2), oneHour, 2.0);
        String json = mapper.writeValueAsString(sample);

        t0 = System.nanoTime();
        for (int i = 0; i < jsonOps; i++) {
            mapper.readValue(json, TimeTable.class);
        }
        t1 = System.nanoTime();
        double d3 = (t1 - t0) / 1_000_000.0;
        out.printf("3. Jackson Deserialization (%d calls):     %6.1f ms (%.3f us/op)%n", jsonOps, d3, ((t1 - t0) / 1000.0) / jsonOps);
        out.println("========================================================");
    }
}
