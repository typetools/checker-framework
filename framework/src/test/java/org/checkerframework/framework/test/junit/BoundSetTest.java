package org.checkerframework.framework.test.junit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.checkerframework.framework.util.typeinference8.bound.BoundSet;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.junit.Assert;
import org.junit.Test;

/** Tests of {@link BoundSet}. */
public class BoundSetTest {

  /**
   * Tests that {@link BoundSet#BoundSet(BoundSet)} copies every field of the bound set, including
   * the two fields that record that inference failed because of a type qualifier: {@code
   * annoInferenceFailed} and {@code errorMsg}.
   *
   * <p>{@code Resolution.resolveSmallestSet} uses the copy constructor to snapshot the bound set
   * before attempting resolution without capture, and it discards the bound set in favor of the
   * snapshot when that attempt fails. A copy constructor that dropped the two annotation-failure
   * fields would lose any qualifier violation that had been recorded before the snapshot was taken,
   * so {@code InferenceResult.inferenceFailed()} could report success with an empty error message
   * even though a qualifier relationship had been violated.
   */
  @Test
  public void copyConstructorCopiesAllFields() {
    BoundSet original = new BoundSet(uninitializedContext());
    Assert.assertFalse(original.annoInferenceFailed);
    Assert.assertEquals("", original.errorMsg);
    Assert.assertFalse(original.containsFalse());
    Assert.assertFalse(original.isUncheckedConversion());

    original.annoInferenceFailed = true;
    original.errorMsg = "@Tainted String <: @Untainted String";
    original.addFalse();
    original.setUncheckedConversion(true);

    BoundSet copy = new BoundSet(original);

    Assert.assertTrue(copy.annoInferenceFailed);
    Assert.assertEquals("@Tainted String <: @Untainted String", copy.errorMsg);
    Assert.assertTrue(copy.containsFalse());
    Assert.assertTrue(copy.isUncheckedConversion());
  }

  /**
   * Returns a {@link Java8InferenceContext} on which no constructor has run, so all its fields are
   * null. Creating a real context requires a running compilation, which is far more than this test
   * needs: {@link BoundSet}'s constructors only store the reference and check that it is non-null.
   *
   * <p>The instance is created the way deserialization creates one, via {@code
   * sun.reflect.ReflectionFactory}. That class is used reflectively so that compiling this file
   * does not produce a "internal proprietary API" warning, which {@code -Werror} would turn into an
   * error.
   *
   * @return a {@link Java8InferenceContext} whose fields are all null
   */
  private static Java8InferenceContext uninitializedContext() {
    try {
      Class<?> reflectionFactoryClass = Class.forName("sun.reflect.ReflectionFactory");
      Object reflectionFactory =
          reflectionFactoryClass.getMethod("getReflectionFactory").invoke(null);
      Method newConstructorForSerialization =
          reflectionFactoryClass.getMethod(
              "newConstructorForSerialization", Class.class, Constructor.class);
      Constructor<?> constructor =
          (Constructor<?>)
              newConstructorForSerialization.invoke(
                  reflectionFactory,
                  Java8InferenceContext.class,
                  Object.class.getDeclaredConstructor());
      return (Java8InferenceContext) constructor.newInstance();
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Could not create a Java8InferenceContext", e);
    }
  }
}
