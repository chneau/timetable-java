package chneau.timetable;

import java.time.LocalDateTime;

final class Point implements Comparable<Point> {
    final LocalDateTime time;
    double value;

    Point(LocalDateTime time, double value) {
        this.time = time;
        this.value = value;
    }

    @Override
    public int compareTo(Point o) {
        var ret = time.compareTo(o.time);
        if (ret == 0) {
            ret = Double.compare(ret, o.value);
        }
        return ret;
    }
}
