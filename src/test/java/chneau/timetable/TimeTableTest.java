/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package chneau.timetable;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import chneau.openhours.OpenHours;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.Test;

public class TimeTableTest {

    @Test
    public void testOverflow() {
        var tt = new TimeTable(1, OpenHours.parse("mo-fr 11:00-16:00"));
        tt = tt.add(LocalDateTime.of(2019, 3, 12, 11, 0), Duration.ofHours(5), 1);
        assertNotNull("tt should be instanciated", tt);
        tt = tt.add(LocalDateTime.of(2019, 3, 12, 12, 0), Duration.ofMillis(1), 0.0001);
        assertNull("tt should be null", tt);
    }

    @Test
    public void testRangeOverlapAtSameTime() {
        var tt = new TimeTable(2, OpenHours.parse("mo-fr 11:00-16:00"));
        tt = tt.add(LocalDateTime.of(2019, 3, 12, 11, 0), Duration.ofHours(1), 1);
        assertNotNull("tt should be instanciated", tt);
        tt = tt.add(LocalDateTime.of(2019, 3, 12, 13, 0), Duration.ofHours(1), 2);
        assertNotNull("tt should be instanciated", tt);
        tt = tt.add(LocalDateTime.of(2019, 3, 13, 11, 0), Duration.ofHours(1), 1);
        assertNotNull("tt should be instanciated", tt);
    }

    @Test
    public void testOverlappingAndSimplifying() {
        var tt = new TimeTable(5, OpenHours.parse("mo-fr 11:00-16:00"));
        for (int i = 0; i < 5; i++) {
            tt = tt.add(LocalDateTime.of(2019, 3, 12, 11, 0), Duration.ofHours(2), 1);
            assertNotNull("tt should be instanciated", tt);
            tt = tt.add(LocalDateTime.of(2019, 3, 12, 13, 0), Duration.ofHours(2), 1);
            assertNotNull("tt should be instanciated", tt);
        }
        tt = tt.add(LocalDateTime.of(2019, 3, 12, 11, 0), Duration.ofHours(2), 1);
        assertNull("tt should be null", tt);
    }

    @Test
    public void testMicroOverflow() {
        var tt = new TimeTable(10, OpenHours.parse("mo-fr 11:00-16:00"));
        var d = LocalDateTime.of(2019, 3, 12, 10, 0);
        for (int i = 0; i < 1000; i++) {
            var when = tt.when(d, Duration.ofHours(1), 1);
            if (when.isAfter(d)) {
                d = when;
            }
            var newtt = tt.add(when, Duration.ofHours(1), 1);
            assertNotNull("tt should be instanciated", tt);
            tt = newtt;
        }
    }
}
