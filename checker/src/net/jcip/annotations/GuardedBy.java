package net.jcip.annotations;

import java.lang.annotation.*;

import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.framework.qual.PreconditionAnnotation;

// The JCIP annotation can be used on a field (in which case it corresponds
// to the Lock Checker's @GuardedBy annotation) or on a method (in which case
// it is a declaration annotation corresponding to the Lock Checker's @Holding
// annotation).
// It is preferred to use these Checker Framework annotations instead:
//  org.checkerframework.checker.lock.qual.GuardedBy
//  org.checkerframework.checker.lock.qual.Holding

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@PreconditionAnnotation(qualifier = LockHeld.class)
public @interface GuardedBy {
    /**
     * The Java expressions that need to be held.
     *
     * @see <a
     *      href="http://types.cs.washington.edu/checker-framework/current/checker-framework-manual.html#java-expressions-as-arguments">Syntax
     *      of Java expressions</a>
     */
    String[] value() default {};
}
