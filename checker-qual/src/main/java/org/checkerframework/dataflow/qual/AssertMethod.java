package org.checkerframework.dataflow.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code AssertMethod} is a method annotation that indicates that a method throws an exception if
 * the value of a boolean argument is false. This can be used to annotate methods such as JUnit's
 * {@code Assertions.assertTrue(...)}.
 *
 * <p>The annotation enables flow-sensitive type refinement to be more precise. For example, if
 * {@code Assertions.assertTrue} is annotated as follows:
 *
 * <pre><code>@AssertMethod(value = AssertionFailedError.class)
 * public static void assertFalse(boolean condition);
 * </code></pre>
 *
 * Then, in the code below, the Optional Checker can determine that {@code optional} has a value and
 * the call to {@code Optional#get} will not throw an exception.
 *
 * <pre>
 * Assertions.assertTrue(optional.isPresent());
 * Object o = optional.get();
 * </pre>
 *
 * <p>This annotation is a <em>trusted</em> annotation, meaning that the Checker Framework does not
 * check whether the annotated method really does throw an exception depending on the boolean
 * expression.
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
   * The one-based index of the boolean parameter that is tested.
   *
   * @return the one-based index of the boolean parameter that is tested
   */
  int parameter() default 1;

  /**
   * Returns whether this method asserts that the boolean expression is false.
   *
   * <p>For example, Junit's <a
   * href="https://junit.org/junit5/docs/5.0.1/api/org/junit/jupiter/api/Assertions.html#assertFalse-boolean-">Assertions.assertFalse(...)</a>
   * throws an exception if the first argument is false. So it is annotated as follows:
   *
   * <pre><code>@AssertMethod(value = AssertionFailedError.class, isAssertFalse = true)
   * public static void assertFalse(boolean condition);
   * </code></pre>
   *
   * @return the value for {@link #parameter} on which the method throws an exception
   */
  boolean isAssertFalse() default false;
}
