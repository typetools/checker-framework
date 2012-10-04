package checkers.util.report.quals;

import java.lang.annotation.*;

/**
 * Report all instantiations of a class/interface that has this annotation,
 * including any subclass.
 * Report all invocations of a particular constructor. (There is no overriding
 * of constructors, so use on a constructor reports only that particular constructor.)
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR})
public @interface ReportCreation{}
