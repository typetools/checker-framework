// Test case for Issue 573:
// https://github.com/typetools/checker-framework/issues/573

// Full test case:
// http://hg.openjdk.java.net/jdk8u/jdk8u/jdk/file/f0851bc0e7bf/src/share/classes/java/time/chrono/Chronology.java

import java.io.Serializable;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.Chronology;
import java.util.Comparator;

public abstract class Issue573 implements Chronology {
  Object o =
      (Comparator<ChronoLocalDateTime<? extends ChronoLocalDate>> & Serializable)
          (dateTime1, dateTime2) -> {
            return 0;
          };
}
