// Adapted test case for https://github.com/typetools/checker-framework/issues/2147 using framework
// package quals

import static java.util.concurrent.TimeUnit.*;

import java.util.concurrent.TimeUnit;
import org.checkerframework.common.util.report.qual.*;

class ParseEnumConstants {

    @SuppressWarnings("report")
    enum MyTimeUnit {
        NANOSECONDS,
        MICROSECONDS,
        @ReportReadWrite
        MILLISECONDS,
        @ReportReadWrite
        SECONDS
    }

    void readFromEnumInSource() {
        // :: error: (fieldreadwrite)
        MyTimeUnit u1 = MyTimeUnit.SECONDS;
        // :: error: (fieldreadwrite)
        MyTimeUnit u2 = MyTimeUnit.MILLISECONDS;

        // these 2 uses should not have any reports
        MyTimeUnit u3 = MyTimeUnit.MICROSECONDS;
        MyTimeUnit u4 = MyTimeUnit.NANOSECONDS;
    }

    void readFromEnumInStub() {
        // :: error: (fieldreadwrite)
        TimeUnit u1 = TimeUnit.SECONDS;
        // :: error: (fieldreadwrite)
        TimeUnit u2 = MILLISECONDS;

        // these 2 uses should not have any reports
        TimeUnit u3 = TimeUnit.MICROSECONDS;
        TimeUnit u4 = NANOSECONDS;

        // :: error: (fieldreadwrite) :: error: (methodcall)
        long sUS = TimeUnit.SECONDS.toMicros(10);
        // :: error: (fieldreadwrite)
        long sNS = SECONDS.toNanos(10);

        // :: error: (fieldreadwrite)
        long msMS = TimeUnit.MILLISECONDS.toMillis(10);
        // :: error: (fieldreadwrite) :: error: (methodcall)
        long msUS = TimeUnit.MILLISECONDS.toMicros(10);
        // :: error: (fieldreadwrite)
        long msNS = MILLISECONDS.toNanos(10);
    }
}
