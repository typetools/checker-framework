/*
 * @test
 * @summary Adapted test case for Issue 2147
 * https://github.com/typetools/checker-framework/issues/2147 using framework package quals
 *
 * @compile/fail/ref=WithoutStub.out -XDrawDiagnostics -processor org.checkerframework.common.util.report.ReportChecker -AstubWarnIfNotFound StubParserEnumTest.java
 * @compile/fail/ref=WithStub.out -XDrawDiagnostics -processor org.checkerframework.common.util.report.ReportChecker -AstubWarnIfNotFound -Astubs=StubParserEnum.astub StubParserEnumTest.java
 */

import static java.util.concurrent.TimeUnit.*;

import java.util.concurrent.TimeUnit;
import org.checkerframework.common.util.report.qual.*;

class StubParserEnumTest {

    @SuppressWarnings("report")
    enum MyTimeUnit {
        NANOSECONDS,
        MICROSECONDS,
        @ReportReadWrite
        MILLISECONDS,
        @ReportReadWrite
        SECONDS;
        
        @ReportCall
        long toMicros(long d) {
            return d;
        }
    }

    void readFromEnumInSource() {
        // :: error: (fieldreadwrite)
        MyTimeUnit u1 = MyTimeUnit.SECONDS;
        // :: error: (fieldreadwrite)
        MyTimeUnit u2 = MyTimeUnit.MILLISECONDS;

        // these 2 uses should not have any reports
        MyTimeUnit u3 = MyTimeUnit.MICROSECONDS;
        MyTimeUnit u4 = MyTimeUnit.NANOSECONDS;
        
        // :: error: (fieldreadwrite) :: error: (methodcall)
        long sUS = MyTimeUnit.SECONDS.toMicros(10);
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
