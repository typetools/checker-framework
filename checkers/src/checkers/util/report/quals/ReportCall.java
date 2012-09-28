package checkers.util.report.quals;

import java.lang.annotation.*;

/**
 * Report all calls of a method that has this annotation, including
 * calls of methods that override this method. Note that calls through
 * a supertype, where the method is not annotated, cannot be reported.
 * 
 * For example, assume three classes A, B, and C, that each implement/override
 * a method m and A <: B <: C.
 * Assume that B.m is annotated as ReportCall.
 * Calls of A.m and B.m will then be reported, but calls of C.m will not
 * be reported, even though the C reference might point to a B object.
 * Therefore, add the ReportCall annotation high enough in the subtype hierarchy.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ReportCall {}
