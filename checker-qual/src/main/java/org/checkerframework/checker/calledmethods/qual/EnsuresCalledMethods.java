package org.checkerframework.checker.calledmethods.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.QualifierArgument;

/**
 * Indicates that the method, if it terminates successfully, always invokes the given methods on the
 * given expressions. This annotation is repeatable, which means that users can write more than one
 * instance of it on the same method (users should NOT manually write an
 * {@code @EnsuresCalledMethods.List} annotation, which the checker will create from multiple copies
 * of this annotation automatically).
 *
 * <p>Consider the following method:
 *
 * <pre>
 * &#64;EnsuresCalledMethods(value = "#1", methods = "m")
 * public void callM(T t) { ... }
 * </pre>
 *
 * <p>This method guarantees that {@code t.m()} is always called before the method returns.
 *
 * <p>If a class has any {@code @}{@link org.checkerframework.checker.mustcall.qual.Owning Owning}
 * fields, then one or more of its must-call methods should be annotated to indicate that the
 * must-call obligations are satisfied. The must-call methods are those named by the {@code @}{@link
 * org.checkerframework.checker.mustcall.qual.MustCall MustCall} or {@code @}{@link
 * org.checkerframework.checker.mustcall.qual.InheritableMustCall InheritableMustCall} annotation on
 * the class declaration, such as {@code close()}. Here is a common example:
 *
 * <pre>
 * &#64;EnsuresCalledMethods(value = {"owningField1", "owningField2"}, methods = "close")
 * public void close() { ... }
 * </pre>
 *
 * @see EnsuresCalledMethodsIf
 * @see EnsuresCalledMethodsOnException
 * @checker_framework.manual #called-methods-checker Called Methods Checker
 */
@PostconditionAnnotation(qualifier = CalledMethods.class)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(EnsuresCalledMethods.List.class)
@InheritedAnnotation
public @interface EnsuresCalledMethods {
  /**
   * The Java expressions that will have methods called on them.
   *
   * @return the Java expressions that will have methods called on them
   * @see org.checkerframework.framework.qual.EnsuresQualifier
   */
  // Postconditions must use "value" as the name (conditional postconditions use "expression").
  String[] value();

  /**
   * The methods guaranteed to be invoked on the expressions.
   *
   * @return the methods guaranteed to be invoked on the expressions
   */
  @QualifierArgument("value")
  String[] methods();

  /**
   * A wrapper annotation that makes the {@link EnsuresCalledMethods} annotation repeatable. This
   * annotation is an implementation detail: programmers generally do not need to write this. It is
   * created automatically by Java when a programmer writes more than one {@link
   * EnsuresCalledMethods} annotation at the same location.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  @InheritedAnnotation
  @PostconditionAnnotation(qualifier = CalledMethods.class)
  public static @interface List {
    /**
     * Return the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    EnsuresCalledMethods[] value();
  }
}
