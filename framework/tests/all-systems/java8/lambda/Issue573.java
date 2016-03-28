// test case for Issue 573
// The full test case reported in the issue is in Issue573FullTest.java in checker/tests/nullness/java8/lambda
// https://github.com/typetools/checker-framework/issues/573
// @below-java8-jdk-skip-test

import java.io.Serializable;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.Chronology;
import java.util.Comparator;

public abstract class Issue573 implements Chronology {
    Object o = (Comparator<ChronoLocalDateTime<? extends ChronoLocalDate>> & Serializable) (dateTime1, dateTime2) -> {
        return 0;
    };
}
