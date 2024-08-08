package org.checkerframework.checker.calledmethodsonelements.qual;

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
 * elements of the given expressions (expression expected to resolve to an array). This annotation
 * is repeatable, which means that users can write more than one instance of it on the same method
 * (users should NOT manually write an {@code @EnsuresCalledMethodsOnElements.List} annotation,
 * which the checker will create from multiple copies of this annotation automatically).
 *
 * <p>Consider the following method:
 *
 * <pre>
 * &#64;EnsuresCalledMethodsOnElements(value = "#1", methods = "m")
 * public void callM(T[] t) { ... }
 * </pre>
 *
 * <p>This method guarantees that {@code m()} is always called on all elements of t before the
 * method returns.
 *
 * <p>If a class has any {@code @}{@link
 * org.checkerframework.checker.mustcallonelements.qual.OwningArray OwningArray} fields, then one or
 * more of its must-call methods should be annotated to indicate that the must-call-on-elements
 * obligations are satisfied. The must-call methods are those named by the {@code @}{@link
 * org.checkerframework.checker.mustcall.qual.MustCall MustCall} or {@code @}{@link
 * org.checkerframework.checker.mustcall.qual.InheritableMustCall InheritableMustCall} annotation on
 * the class declaration, such as {@code close()}. Here is a common example:
 *
 * <pre>
 * &#64;EnsuresCalledMethodsOnElements(value = {"owningArrayField1", "owningArrayField2"}, methods = "close")
 * public void close() { ... }
 * </pre>
 *
 * @checker_framework.manual #resource-leak-checker Resource Leak Checker
 */
@PostconditionAnnotation(qualifier = CalledMethodsOnElements.class)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(EnsuresCalledMethodsOnElements.List.class)
@InheritedAnnotation
public @interface EnsuresCalledMethodsOnElements {
  /**
   * The Java expressions that will have methods called on their elements.
   *
   * @return the Java expressions that will have methods called on their elements
   * @see org.checkerframework.framework.qual.EnsuresQualifier
   */
  // Postconditions must use "value" as the name (conditional postconditions use "expression").
  String[] value();

  /**
   * The methods guaranteed to be invoked on the elements of expressions.
   *
   * @return the methods guaranteed to be invoked on the elements of expressions
   */
  @QualifierArgument("value")
  String[] methods();

  /**
   * A wrapper annotation that makes the {@link EnsuresCalledMethodsOnElements} annotation
   * repeatable. This annotation is an implementation detail: programmers generally do not need to
   * write this. It is created automatically by Java when a programmer writes more than one {@link
   * EnsuresCalledMethodsOnElements} annotation at the same location.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  @InheritedAnnotation
  @PostconditionAnnotation(qualifier = CalledMethodsOnElements.class)
  public static @interface List {
    /**
     * Return the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    EnsuresCalledMethodsOnElements[] value();
  }
}
