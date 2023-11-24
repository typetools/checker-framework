package org.checkerframework.dataflow.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code AssertMethod} is a method annotation that indicates that a method throws an exception if
 * the value of a boolean argument is false. This can be used to annotate methods such as Junit's
 * {@code Assertions.assertTrue(...)}.
 *
 * <p>The annotation enables flow-sensitive type refinement to be more precise. For example, after
 *
 * <pre>
 *   Assertions.assertTrue(optional.isPresent());
 * </pre>
 *
 * the Optional Checker can determine that {@code optional} has a value.
 *
 * <p>This annotation is a <em>trusted</em> annotation, meaning that it is not checked whether the
 * annotated method really does throw an exception if the boolean expression is true.
 *
 * @checker_framework.manual #type-refinement Automatic type refinement (flow-sensitive type
 *     qualifier inference)
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AssertMethod {

  /**
   * The class of the exception thrown by this method. The default is {@link AssertionError}.
   *
   * @return class of the exception thrown by this method
   */
  Class<?> value() default AssertionError.class;

  /**
   * The one-based index of the boolean parameter that is tested. If the parameter is false, the
   * method throws an exception. The check can be reversed by use of the {@link #result} element; if
   * {@link result} is false, then the method throws an exception if the parameter is true.
   *
   * @return the one-based index of the boolean parameter that is tested
   */
  int parameter() default 1;

  /**
   * On which value for {@link #parameter} does the method throw an exception?
   *
   * @return the result for {@link #parameter} on which the method throws an exception
   */
  boolean result() default true;
}
