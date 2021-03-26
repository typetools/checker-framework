/*
 * @test
 * @summary Adapted test case for Issue 2147
 * https://github.com/typetools/checker-framework/issues/2147 using framework package quals
 *
 * @compile/fail/ref=WithoutStub.out -XDrawDiagnostics -processor org.checkerframework.common.util.report.ReportChecker -AstubWarnIfNotFound AnnotationFileParserEnumTest.java
 * @compile/fail/ref=WithStub.out -XDrawDiagnostics -processor org.checkerframework.common.util.report.ReportChecker -AstubWarnIfNotFound -Astubs=AnnotationFileParserEnum.astub AnnotationFileParserEnumTest.java
 */

import static java.util.concurrent.TimeUnit.*;

import java.util.concurrent.TimeUnit;
import org.checkerframework.common.util.report.qual.*;

public class AnnotationFileParserEnumTest {

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
    MyTimeUnit u1 = MyTimeUnit.SECONDS;
    MyTimeUnit u2 = MyTimeUnit.MILLISECONDS;
    MyTimeUnit u3 = MyTimeUnit.MICROSECONDS;
    MyTimeUnit u4 = MyTimeUnit.NANOSECONDS;
    long sUS = MyTimeUnit.SECONDS.toMicros(10);
  }

  void readFromEnumInStub() {
    TimeUnit u1 = TimeUnit.SECONDS;
    TimeUnit u2 = MILLISECONDS;
    TimeUnit u3 = TimeUnit.MICROSECONDS;
    TimeUnit u4 = NANOSECONDS;
    long sUS = TimeUnit.SECONDS.toMicros(10);
    long sNS = SECONDS.toNanos(10);
    long msMS = TimeUnit.MILLISECONDS.toMillis(10);
    long msUS = TimeUnit.MILLISECONDS.toMicros(10);
    long msNS = MILLISECONDS.toNanos(10);
  }
}
