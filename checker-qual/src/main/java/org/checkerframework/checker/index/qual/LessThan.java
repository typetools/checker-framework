package org.checkerframework.checker.index.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An annotation indicating the relationship between values with a byte, short, char, int, or long
 * type.
 *
 * <p>If an expression's type has this annotation, then at run time, the expression evaluates to a
 * value that is less than the value of the expression in the annotation.
 *
 * <p>Subtyping:
 *
 * <ul>
 *   <li>{@code @LessThan({"a", "b"}) <: @LessThan({"a"})}
 *   <li>{@code @LessThan({"a", "b"})} is not related to {@code @LessThan({"a", "c"})}.
 * </ul>
 *
 * @checker_framework.manual #index-inequalities Index Checker Inequalities
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@SubtypeOf({LessThanUnknown.class})
// TODO: I chose to implement less than rather than greater than because in most of the case studies
// false positives, the bigger value is final or effectively final, so it can appear in a dependent
// annotation without causing soundness issues.
public @interface LessThan {
  /**
   * The annotated expression's value is less than this expression.
   *
   * <p>The expressions in {@code value} may be addition/subtraction of any number of Java
   * expressions. For example, {@code @LessThan(value = "x + y + 2"}}.
   *
   * <p>The expression in {@code value} must be final or constant or the addition/subtract of final
   * or constant expressions.
   */
  @JavaExpression
  String[] value();
}
