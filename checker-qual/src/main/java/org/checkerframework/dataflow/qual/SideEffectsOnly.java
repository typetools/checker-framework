package org.checkerframework.dataflow.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;

/**
 * A method annotated with the declaration annotation {@code @SideEffectsOnly({"A", "B"})} changes
 * the value of at most the expressions A and B. No other expression is directly modified by the
 * method. Absent aliasing, no other expression has a different value after a call to the method.
 * But checking of this annotation (under {@code -AcheckPurityAnnotations}) treats two expressions
 * as possibly aliased only when an assignment relating them appears in the method body.
 *
 * <p>This annotation is inherited by subtypes, just as if it were meta-annotated with
 * {@code @InheritedAnnotation}.
 *
 * <p>On a constructor, this annotation constrains what the constructor modifies besides the object
 * being constructed; list {@code this} to permit assigning to the new object's own fields. A
 * constructor's annotation is verified at its declaration, but it does not yet affect type
 * refinement at {@code new} expressions.
 *
 * @checker_framework.manual #type-refinement-purity Specifying side effects
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface SideEffectsOnly {
  /**
   * An upper bound on the expressions that this method might change the value of.
   *
   * @return the Java expressions that the annotated method might side-effect
   * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
   */
  @JavaExpression
  public String[] value();
}
