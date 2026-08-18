package org.checkerframework.framework.util.typeinference8;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Creates an instance of a class on which no constructor has run. Type inference unit tests use
 * such an instance in place of an object that could otherwise be created only by a running
 * compilation, when the test uses none of the object's fields.
 */
public final class UninitializedInstance {

  /** Do not instantiate. */
  private UninitializedInstance() {
    throw new AssertionError("Class UninitializedInstance cannot be instantiated.");
  }

  /**
   * Returns an instance of {@code clazz} on which no constructor has run, so all its fields have
   * their default values.
   *
   * <p>The instance is created the way deserialization creates one, via {@code
   * sun.reflect.ReflectionFactory}. That class is used reflectively so that compiling this file
   * does not produce a "internal proprietary API" warning, which {@code -Werror} would turn into an
   * error.
   *
   * @param <T> the type of the instance to create
   * @param clazz the class of the instance to create
   * @return an instance of {@code clazz} whose fields all have their default values
   */
  public static <T> T uninitialized(Class<T> clazz) {
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
                  reflectionFactory, clazz, Object.class.getDeclaredConstructor());
      return clazz.cast(constructor.newInstance());
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Could not create a " + clazz.getSimpleName(), e);
    }
  }
}
