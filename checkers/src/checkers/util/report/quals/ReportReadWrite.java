package checkers.util.report.quals;

import java.lang.annotation.*;

/**
 * Report all read or write access to a field with this annotation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ReportReadWrite {}
