package checkers.util.report.quals;

import java.lang.annotation.*;

/**
 * Report all write accesses to a field with this annotation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ReportWrite {}
