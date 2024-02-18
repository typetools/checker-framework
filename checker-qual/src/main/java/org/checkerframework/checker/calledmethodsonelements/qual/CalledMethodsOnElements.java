package org.checkerframework.checker.calledmethodsonelements.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * If an expression has type {@code @CalledMethodsOnElements({"m1", "m2"})}, then methods {@code m1}
 * and {@code m2} have definitely been called on the elements of the value, which must be an array.
 * Other methods might or might not have been called. "Been called" is defined as having been
 * invoked: a method has "been called" even if it might never return or might throw an exception.
 *
 * <p>The subtyping relationship is:
 *
 * <pre>
 * {@code @CalledMethodsOnElements({"m1", "m2", "m3"}) <: @CalledMethodsOnElements({"m1", "m2"})}
 * </pre>
 *
 * @checker_framework.manual #called-methods-checker Called Methods Checker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface CalledMethodsOnElements {
  /**
   * Methods that have definitely been called on the elements of the array expression whose type is
   * annotated.
   *
   * @return methods that have definitely been called
   */
  public String[] value() default {};
}
