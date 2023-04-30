package org.checkerframework.checker.lock.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * If a variable {@code x} has type {@code @GuardSatisfied}, then all lock expressions for {@code
 * x}'s value are held.
 *
 * <p>Written on a formal parameter (including the receiver), this annotation indicates that the
 * {@literal @}{@link GuardedBy} type for the corresponding actual argument at the method call site
 * is unknown at the method definition site, but any lock expressions that guard it are known to be
 * held prior to the method call.
 *
 * <p>For example, the formal parameter of the String copy constructor, {@link String#String(String
 * s)}, is annotated with {@code @GuardSatisfied}. This requires that all locks guarding the actual
 * argument are held when the constructor is called. However, the definition of the constructor does
 * not need to know what those locks are (and it cannot know, because the constructor can be called
 * by arbitrary code).
 *
 * @see GuardedBy
 * @see Holding
 * @checker_framework.manual #lock-checker Lock Checker
 * @checker_framework.manual #lock-checker-polymorphism-example Lock Checker polymorphism example
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
@TargetLocations({TypeUseLocation.RECEIVER, TypeUseLocation.PARAMETER, TypeUseLocation.RETURN})
@SubtypeOf(GuardedByUnknown.class) // TODO: Should @GuardSatisfied be in its own hierarchy?
public @interface GuardSatisfied {
  /**
   * The index on the GuardSatisfied polymorphic qualifier, if any. Defaults to -1 so that, if the
   * user writes 0, that is different than writing no index. Writing no index is the usual case.
   *
   * @return the index on the GuardSatisfied polymorphic qualifier, or -1 if none
   */
  int value() default -1;
}
