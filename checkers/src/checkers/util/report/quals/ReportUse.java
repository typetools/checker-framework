package checkers.util.report.quals;

import java.lang.annotation.*;

/**
 * Report all uses of a type that has this annotation.
 * Can also be used on a package.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PACKAGE, ElementType.TYPE})
public @interface ReportUse {}
