// Test case for https://github.com/typetools/checker-framework/issues/2147

import org.checkerframework.common.util.report.qual.*;

import java.util.concurrent.TimeUnit;

class ParseEnumConstants {

    @SuppressWarnings("report")
    enum MyTimeUnit {
        NANOSECONDS,
        MICROSECONDS,
        @ReportReadWrite MILLISECONDS,
        @ReportReadWrite SECONDS
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

    void ReadFromEnumStub(){
        // :: error: (fieldreadwrite)
        TimeUnit u1 = TimeUnit.SECONDS;
        // :: error: (fieldreadwrite)
        TimeUnit u2 = TimeUnit.MILLISECONDS;

        // these 2 uses should not have any reports
        TimeUnit u3 = TimeUnit.MICROSECONDS;
        TimeUnit u4 = TimeUnit.NANOSECONDS;
    }
}
