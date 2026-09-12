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
 * being constructed. Assigning to the new object's own fields is always permitted and need not be
 * listed, because the object did not exist before the call; writing {@code this} in the annotation
 * is legal but has no additional effect. At a {@code new} expression, the expressions that are
 * reached through {@code this} are ignored, because the object being constructed did not exist
 * before the call. A constructor's annotation does not yet affect type refinement at {@code new}
 * expressions.
 *
 * @checker_framework.manual #purity-trusted Checking {@code @SideEffectsOnly}
 */
// TODO: The manual does not yet document @SideEffectsOnly:  its text is inside \iffalse in
// purity-checker.tex and advanced-features.tex, so there is no #side-effects-only-checking
// anchor and the link below is broken.  Restore this tag in place of the #purity-trusted one
// above when the feature ships and the manual documents it:
// @checker_framework.manual #side-effects-only-checking Checking {@code @SideEffectsOnly}
// @InheritedAnnotation cannot be written here, because "dataflow" project cannot depend on
// "framework" project.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface SideEffectsOnly {
  /**
   * An upper bound on the expressions that this method might change the value of.
   *
   * <p>Each expression must denote the same location every time it is evaluated: it must be a
   * variable, a field access, an array access, a literal, a class name, or a call to a {@link Pure}
   * method, recursively. A {@code @Pure} method returns the same value every time it is called with
   * the same arguments, so a call to one qualifies so long as its receiver and its arguments do. An
   * expression such as {@code "#1.getList()"}, where {@code getList} is not {@code @Pure}, may
   * denote a different value each time it is evaluated, so no method body could satisfy it.
   *
   * @return the Java expressions that the annotated method might side-effect
   * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
   */
  @JavaExpression
  String[] value();
}
