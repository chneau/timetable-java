package chneau.timetable;

import chneau.openhours.Whenable;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Modern, high-performance capacity and timeline constraint engine for Java 26+.
 */
@JsonSerialize(using = TimeTable.Serializer.class)
@JsonDeserialize(using = TimeTable.Deserializer.class)
public final class TimeTable {
    private static final double EPSILON = 1e-9;

    private final List<Point> rel;
    private final Whenable contraint;
    private final double max;

    public TimeTable(double max) {
        this(max, null);
    }

    public TimeTable(double max, Whenable contraint) {
        this(max, contraint, List.of());
    }

    private TimeTable(double max, Whenable contraint, List<Point> rel) {
        this.max = max;
        this.contraint = (contraint == null) ? NoopWhen.getInstance() : contraint;
        this.rel = (rel == null || rel.isEmpty()) ? List.of() : List.copyOf(rel);
    }

    public double getMax() {
        return max;
    }

    public Whenable getConstraint() {
        return contraint;
    }

    public List<Point> getPoints() {
        return rel;
    }

    public int size() {
        return rel.size();
    }

    public boolean isEmpty() {
        return rel.isEmpty();
    }

    private boolean check() {
        double current = 0.0;
        int n = rel.size();
        for (int i = 0; i < n; i++) {
            current += rel.get(i).value();
            if (current > max + EPSILON) {
                return false;
            }
        }
        return Math.abs(current) <= EPSILON;
    }

    private static List<Point> simplifyList(List<Point> points) {
        if (points.isEmpty()) {
            return List.of();
        }
        var map = new TreeMap<LocalDateTime, Double>();
        for (Point p : points) {
            map.merge(p.time(), p.value(), Double::sum);
        }

        var result = new ArrayList<Point>(map.size());
        for (Map.Entry<LocalDateTime, Double> entry : map.entrySet()) {
            double val = entry.getValue();
            if (Math.abs(val) > EPSILON) {
                result.add(new Point(entry.getKey(), val));
            }
        }
        return Collections.unmodifiableList(result);
    }

    public TimeTable add(LocalDateTime from, Duration dur, double cap) {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(dur, "dur must not be null");
        if (dur.isNegative() || dur.isZero()) {
            throw new IllegalArgumentException("duration must be positive");
        }

        int n = rel.size();
        var x = new ArrayList<Point>(n + 2);
        x.addAll(rel);
        x.add(new Point(from, cap));
        x.add(new Point(from.plus(dur), -cap));
        x.sort(null);

        var simplified = simplifyList(x);
        var tt = new TimeTable(max, contraint, simplified);
        if (!tt.check()) {
            return null;
        }
        return tt;
    }

    public LocalDateTime when(LocalDateTime ldt, Duration d, double cap) {
        Objects.requireNonNull(ldt, "ldt must not be null");
        Objects.requireNonNull(d, "d must not be null");

        LocalDateTime candidate = contraint.when(ldt, d);
        if (candidate != null) {
            ldt = candidate;
        }

        if (add(ldt, d, cap) != null) {
            return ldt;
        }

        int n = rel.size();
        for (int i = 0; i < n; i++) {
            Point p = rel.get(i);
            if (!p.time().isAfter(ldt)) {
                continue;
            }
            LocalDateTime nextTime = p.time();
            LocalDateTime constrained = contraint.when(nextTime, d);
            if (constrained != null) {
                nextTime = constrained;
            }
            TimeTable test = add(nextTime, d, cap);
            if (test != null && test.check()) {
                return nextTime;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return rel.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeTable timeTable = (TimeTable) o;
        return Double.compare(timeTable.max, max) == 0 && Objects.equals(rel, timeTable.rel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rel, max);
    }

    public static final class Serializer extends JsonSerializer<TimeTable> {
        @Override
        public void serialize(TimeTable value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            gen.writeStartObject();
            gen.writeNumberField("max", value.max);
            gen.writeArrayFieldStart("points");
            for (Point p : value.rel) {
                gen.writeStartObject();
                gen.writeStringField("time", p.time().toString());
                gen.writeNumberField("value", p.value());
                gen.writeEndObject();
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }
    }

    public static final class Deserializer extends JsonDeserializer<TimeTable> {
        @Override
        public TimeTable deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            com.fasterxml.jackson.databind.JsonNode node = p.getCodec().readTree(p);
            double maxVal = node.has("max") ? node.get("max").asDouble() : 0.0;
            com.fasterxml.jackson.databind.JsonNode pointsNode = node.get("points");
            var points = new ArrayList<Point>();
            if (pointsNode != null && pointsNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode item : pointsNode) {
                    var timeStr = item.get("time").asText();
                    var value = item.get("value").asDouble();
                    points.add(new Point(LocalDateTime.parse(timeStr), value));
                }
            }
            return new TimeTable(maxVal, NoopWhen.getInstance(), simplifyList(points));
        }
    }
}

