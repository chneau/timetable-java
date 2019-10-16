package chneau.timetable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import chneau.openhours.Whenable;

public final class TimeTable{
    private List<Point> rel;
    private final Whenable contraint;
    private final double max;

    public TimeTable(double max) {
        this(max, null);
    }

    public TimeTable(double max, Whenable contraint) {
        this(max, contraint, new ArrayList<>());
    }

    private TimeTable(double max, Whenable contraint, List<Point> rel) {
        this.rel = rel;
        this.max = max;
        if (contraint == null) {
            contraint = NoopWhen.getInstance();
        }
        this.contraint = contraint;
    }

    public boolean check() {
        var res = 0;
        for (int i = 0; i < rel.size(); i++) {
            res += rel.get(i).value;
            if (res > max) {
                return false;
            }
        }
        return true;
    }

    private void simplify() {
        var newx = new ArrayList<Point>();
        for (int i = 0; i < rel.size(); i++) {
            if (i == 0) {
                newx.add(rel.get(i));
                continue;
            }
            var relTime = rel.get(i).time;
            var newxTime = newx.get(newx.size() - 1).time;
            if (relTime.equals(newxTime)) {
                newx.get(newx.size() - 1).value += rel.get(i).value;
                if (newx.get(newx.size() - 1).value == 0) {
                    newx.remove(newx.size() - 1);
                }
            } else {
                newx.add(rel.get(i));
            }
        }
        this.rel = newx;
    }

    public TimeTable add(LocalDateTime from, Duration dur, double cap) {
        var x = new ArrayList<>(rel);
        x.add(new Point(from, cap));
        x.add(new Point(from.plus(dur), -cap));
        Collections.sort(x);
        var tt = new TimeTable(max, this.contraint, x);
        tt.simplify();
        return tt;
    }

    public LocalDateTime when(LocalDateTime ldt, Duration d, double cap) {
        {
            var t = contraint.when(ldt, d);
            if (t != null) {
                ldt = t;
            }
        }
        if (add(ldt, d, cap).check()) {
            return ldt;
        }
        for (int i = 0; i < rel.size(); i++) {
            if (rel.get(i).time.isAfter(ldt)) {
                ldt = rel.get(i).time;
            } else {
                continue;
            }
            var t = contraint.when(ldt, d);
            if (t != null) {
                ldt = t;
            }
            if (add(ldt, d, cap).check()) {
                return ldt;
            }
        }
        return null;
    }
}
