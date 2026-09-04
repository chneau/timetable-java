package chneau.timetable;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public record Point(LocalDateTime time, double value) implements Comparable<Point> {
    public Point {
        if (time == null) {
            throw new NullPointerException("time must not be null");
        }
    }

    public long epochSecond() {
        return time.toEpochSecond(ZoneOffset.UTC);
    }

    @Override
    public int compareTo(Point o) {
        int ret = time.compareTo(o.time);
        if (ret == 0) {
            ret = Double.compare(value, o.value);
        }
        return ret;
    }

    @Override
    public String toString() {
        return "{" + time + "," + value + "}";
    }
}

