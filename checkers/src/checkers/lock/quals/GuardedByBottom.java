
package checkers.lock.quals;

import java.lang.annotation.*;

import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * The bottom of the guarded-by qualifier hierarchy.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 *
 * @checker_framework_manual #lock-checker Lock Checker
 */
@TypeQualifier
@SubtypeOf(GuardedBy.class)
@DefaultQualifierInHierarchy
@Documented
@Target({}) // not necessary to be used by the programmer
@Retention(RetentionPolicy.RUNTIME)
public @interface GuardedByBottom {}
