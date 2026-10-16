package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.QualifierArgument;

/**
 * Indicates that the value expressions evaluate to a value that is a key in all the given maps, if
 * the method terminates successfully.
 *
 * <p>Consider the following method from {@code java.util.Map}:
 *
 * <pre>
 * &#64;EnsuresKeyFor(value="key", map="this")
 * public @Nullable V put(K key, V value) { ... }
 * </pre>
 *
 * <p>This method guarantees that {@code key} has type {@code @KeyFor("this")} after the method
 * returns.
 *
 * @see KeyFor
 * @see EnsuresKeyForIf
 * @checker_framework.manual #map-key-checker Map Key Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@PostconditionAnnotation(qualifier = KeyFor.class)
@InheritedAnnotation
@Repeatable(EnsuresKeyFor.List.class)
public @interface EnsuresKeyFor {
  /**
   * Java expressions that are keys in the given maps on successful method termination.
   *
   * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
   */
  String[] value();

  /**
   * Returns Java expressions whose values are maps, each of which contains each expression value as
   * a key (after successful method termination).
   *
   * @return Java expressions whose values are maps, each of which contains each expression value as
   *     a key (after successful method termination)
   * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
   */
  @JavaExpression
  @QualifierArgument("value")
  String[] map();

  /**
   * A wrapper annotation that makes the {@link EnsuresKeyFor} annotation repeatable.
   *
   * <p>Programmers generally do not need to write this. It is created by Java when a programmer
   * writes more than one {@link EnsuresKeyFor} annotation at the same location.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  @PostconditionAnnotation(qualifier = KeyFor.class)
  @InheritedAnnotation
  public static @interface List {
    /**
     * Returns the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    EnsuresKeyFor[] value();
  }
}
