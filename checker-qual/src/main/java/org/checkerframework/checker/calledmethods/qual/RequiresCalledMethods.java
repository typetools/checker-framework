package org.checkerframework.checker.calledmethods.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PreconditionAnnotation;
import org.checkerframework.framework.qual.QualifierArgument;

/**
 * Indicates a method precondition: the method expects the specified expressions have definitely had
 * the specified methods called on them at the point when the annotated method is invoked.
 *
 * <p>Do not use this annotation for formal parameters (instead, give them a {@code @CalledMethods}
 * type). The {@code @RequiresCalledMethods} annotation is intended for other expressions, such as
 * field accesses or method calls.
 *
 * @checker_framework.manual #called-methods-checker Called Methods Checker
 */
@PreconditionAnnotation(qualifier = CalledMethods.class)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface RequiresCalledMethods {
  /**
   * The Java expressions to which the qualifier applies.
   *
   * @return the Java expressions to which the qualifier applies
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
}
