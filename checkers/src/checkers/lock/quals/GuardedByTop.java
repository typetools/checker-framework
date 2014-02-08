
package checkers.lock.quals;

import java.lang.annotation.*;

import checkers.quals.InvisibleQualifier;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * The top of the guarded-by qualifier hierarchy.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 *
 * @checker_framework_manual #lock-checker Lock Checker
 */
@TypeQualifier
@InvisibleQualifier
@SubtypeOf({})
@Documented
@Target({}) // not necessary to be used by the programmer
@Retention(RetentionPolicy.RUNTIME)
public @interface GuardedByTop {}
