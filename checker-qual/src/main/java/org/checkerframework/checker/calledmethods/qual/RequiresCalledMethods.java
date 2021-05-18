package org.checkerframework.checker.calledmethods.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PreconditionAnnotation;
import org.checkerframework.framework.qual.QualifierArgument;

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
