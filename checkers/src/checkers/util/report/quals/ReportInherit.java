package checkers.util.report.quals;

import java.lang.annotation.*;

/**
 * Report all types that extend/implement a type that has this annotation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ReportInherit {}
