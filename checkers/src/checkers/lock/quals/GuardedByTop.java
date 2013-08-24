
package checkers.lock.quals;

import java.lang.annotation.*;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * The top of the guarded-by qualifier hierarchy.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 *
 * @checker.framework.manual #lock-checker Lock Checker
 */
@Documented
@Target({}) // not necessary to be used by the programmer
@Retention(RetentionPolicy.RUNTIME)
@TypeQualifier
@SubtypeOf({})
public @interface GuardedByTop {
}
