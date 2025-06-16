package org.checkerframework.checker.calledmethods.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InheritedAnnotation;

/**
 * Indicates that the method, if it terminates by throwing an {@link Exception}, always invokes the
 * given methods on the given expressions. This annotation is repeatable, which means that users can
 * write more than one instance of it on the same method (users should NOT manually write an
 * {@code @EnsuresCalledMethodsOnException.List} annotation, which the checker will create from
 * multiple copies of this annotation automatically).
 *
 * <p>Consider the following method:
 *
 * <pre>
 * &#64;EnsuresCalledMethodsOnException(value = "#1", methods = "m")
 * public void callM(T t) { ... }
 * </pre>
 *
 * <p>The <code>callM</code> method promises to always call {@code t.m()} before throwing any kind
 * of {@link Exception}.
 *
 * <p>Note that {@code EnsuresCalledMethodsOnException} only describes behavior for {@link
 * Exception} (and by extension {@link RuntimeException}, {@link NullPointerException}, etc.) but
 * not {@link Error} or other throwables.
 *
 * @see EnsuresCalledMethods
 * @checker_framework.manual #called-methods-checker Called Methods Checker
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(EnsuresCalledMethodsOnException.List.class)
@Retention(RetentionPolicy.RUNTIME)
@InheritedAnnotation
public @interface EnsuresCalledMethodsOnException {

  /**
   * Returns Java expressions that have had the given methods called on them after the method throws
   * an exception.
   *
   * @return an array of Java expressions
   * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
   */
  String[] value();

  // NOTE 2023/10/6: There seems to be a fundamental limitation in the dataflow framework that
  // prevent us from supporting a custom set of exceptions.  Specifically, in the following code:
  //
  //     try {
  //       m1();
  //     } finally {
  //       m2();
  //     }
  //
  // all exceptional edges out of the `m1()` call will flow to the same place: the start of the
  // `m2()` call in the finally block.  Any information about what `m1()` promised on specific
  // exception types will be lost.
  //
  //  /**
  //   * Returns the exception types under which the postcondition holds.
  //   *
  //   * @return the exception types under which the postcondition holds
  //   */
  //  Class<? extends Throwable>[] exceptions();

  /**
   * The methods guaranteed to be invoked on the expressions if the result of the method throws an
   * exception.
   *
   * @return the methods guaranteed to be invoked on the expressions if the method throws an
   *     exception
   */
  String[] methods();

  /**
   * A wrapper annotation that makes the {@link EnsuresCalledMethodsOnException} annotation
   * repeatable. This annotation is an implementation detail: programmers generally do not need to
   * write this. It is created automatically by Java when a programmer writes more than one {@link
   * EnsuresCalledMethodsOnException} annotation at the same location.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  @InheritedAnnotation
  public static @interface List {
    /**
     * Return the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    EnsuresCalledMethodsOnException[] value();
  }
}
