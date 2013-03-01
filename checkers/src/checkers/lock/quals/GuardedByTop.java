
package checkers.lock.quals;

import java.lang.annotation.*;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * The top of the guarded-by qualifier hierarchy (used internally only).
 */
@Documented
@Target({}) // not necessary to be used by the programmer
@Retention(RetentionPolicy.RUNTIME)
@TypeQualifier
@SubtypeOf({})
public @interface GuardedByTop {
}
