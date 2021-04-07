package org.checkerframework.checker.mustcall.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * If an expression has type {@code @MustCall({"m1", "m2"})}, then the Object Construction Checker's
 * {@code -AcheckMustCall} mode will enforce that the methods "m1" and "m2" are called on the
 * annotated value before it is deallocated.
 *
 * <p>The subtyping relationship is:
 *
 * <pre>{@code @MustCall({"m1"}) <: @MustCall({"m1", "m2"})}</pre>
 *
 * <p>In practice, this means that value of a type that is annotated with {@code @MustCall({"m1",
 * "m2"})} may be obligated to call "m1" and/or "m2" before it is deallocated. Such a value is
 * guaranteed not to be obligated to call any other methods.
 *
 * @checker_framework.manual #must-call-checker Must Call Checker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({MustCallUnknown.class})
@DefaultQualifierInHierarchy
@DefaultFor({TypeUseLocation.EXCEPTION_PARAMETER})
public @interface MustCall {
  /**
   * Methods that must be called, on any expression whose type is annotated.
   *
   * @return methods that must be called
   */
  public String[] value() default {};
}
