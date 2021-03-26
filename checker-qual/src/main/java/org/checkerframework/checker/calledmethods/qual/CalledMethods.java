package org.checkerframework.checker.calledmethods.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * If an expression has type {@code @CalledMethods({"m1", "m2"})}, then methods {@code m1} and
 * {@code m2} have definitely been called on its value. Other methods might or might not have been
 * called.
 *
 * <p>The subtyping relationship is:
 *
 * <pre>{@code @CalledMethods({"m1", "m2", "m3"}) <: @CalledMethods({"m1", "m2"})}</pre>
 *
 * @checker_framework.manual #called-methods-checker Called Methods Checker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface CalledMethods {
  /**
   * Methods that have definitely been called, on any expression whose type is annotated.
   *
   * @return methods that have definitely been called
   */
  public String[] value() default {};
}
