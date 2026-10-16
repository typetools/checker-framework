package org.checkerframework.checker.calledmethods.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PreconditionAnnotation;
import org.checkerframework.framework.qual.QualifierArgument;

/**
 * Indicates a method precondition: when the method is invoked, the specified expressions must have
 * had the specified methods called on them.
 *
 * <p>Do not use this annotation for formal parameters; instead, give their type a {@code @}{@link
 * CalledMethods} annotation. The {@code @RequiresCalledMethods} annotation is intended for other
 * expressions, such as field accesses or method calls.
 *
 * @checker_framework.manual #called-methods-checker Called Methods Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@PreconditionAnnotation(qualifier = CalledMethods.class)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(RequiresCalledMethods.List.class)
public @interface RequiresCalledMethods {
  /**
   * The Java expressions that must have had methods called on them.
   *
   * @return the Java expressions that must have had methods called on them
   * @see org.checkerframework.framework.qual.EnsuresQualifier
   */
  // Preconditions must use "value" as the name (conditional preconditions use "expression").
  String[] value();

  /**
   * The methods guaranteed to be invoked on the expressions.
   *
   * @return the methods guaranteed to be invoked on the expressions
   */
  @QualifierArgument("value")
  String[] methods();

  /**
   * A wrapper annotation that makes the {@link RequiresCalledMethods} annotation repeatable.
   *
   * <p>Programmers generally do not need to write this. It is created by Java when a programmer
   * writes more than one {@link RequiresCalledMethods} annotation at the same location.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @PreconditionAnnotation(qualifier = CalledMethods.class)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  public static @interface List {
    /**
     * Returns the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    RequiresCalledMethods[] value();
  }
}
