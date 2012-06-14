package checkers.util.report.quals;

import java.lang.annotation.*;

/**
 * Report all methods that override a method with this annotation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ReportOverride {}
