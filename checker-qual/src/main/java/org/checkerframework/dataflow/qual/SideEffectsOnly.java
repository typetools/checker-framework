package org.checkerframework.dataflow.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;

/**
 * A method annotated with the declaration annotation {@code @SideEffectsOnly(A, B)} performs
 * side-effects on at most the expressions A and B. All other expressions have the same value before
 * and after a call.
 *
 * @checker_framework.manual #type-refinement-purity Specifying side effects
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface SideEffectsOnly {
  /**
   * An upper bound on the expressions that this method side effects.
   *
   * @return Java expressions that the annotated method might side-effect
   * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
   */
  @JavaExpression
  public String[] value();
}