// Upstream version (this is a clean-room reimplementation of its interface):
// https://jcip.net/annotations/doc/net/jcip/annotations/GuardedBy.html

package net.jcip.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.framework.qual.PreconditionAnnotation;

// The JCIP annotation can be used on a field (in which case it corresponds to the Lock Checker's
// @GuardedBy annotation) or on a method (in which case it is a declaration annotation corresponding
// to the Lock Checker's @Holding annotation).
// It is preferred to use these Checker Framework annotations instead:
//  org.checkerframework.checker.lock.qual.GuardedBy
//  org.checkerframework.checker.lock.qual.Holding

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@PreconditionAnnotation(qualifier = LockHeld.class)
public @interface GuardedBy {
  /**
   * The Java expressions that need to be held.
   *
   * @see <a href="https://checkerframework.org/manual/#java-expressions-as-arguments">Syntax of
   *     Java expressions</a>
   */
  String[] value() default {};
}
