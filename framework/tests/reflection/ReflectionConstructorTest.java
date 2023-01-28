import java.lang.reflect.Constructor;
import org.checkerframework.framework.testchecker.reflection.qual.TestReflectSibling1;
import org.checkerframework.framework.testchecker.reflection.qual.TestReflectSibling2;
import org.checkerframework.framework.testchecker.reflection.qual.TestReflectTop;

public class ReflectionConstructorTest {
  @TestReflectSibling1 int sibling1;
  @TestReflectSibling2 int sibling2;

  // :: error: (super.invocation) :: warning: (inconsistent.constructor.type)
  public @TestReflectSibling1 ReflectionConstructorTest(@TestReflectSibling1 int a) {}

  // :: warning: (inconsistent.constructor.type)
  public @TestReflectSibling2 ReflectionConstructorTest(
      // :: error: (super.invocation)
      @TestReflectSibling2 int a, @TestReflectSibling2 int b) {}

  public void pass1() {
    try {
      Class<?> c = Class.forName("ReflectionConstructorTest");
      Constructor<?> init = c.getConstructor(new Class<?>[] {Integer.class});
      @TestReflectSibling1 int i = sibling1;
      @TestReflectSibling1 Object o = init.newInstance(i);
    } catch (Exception ignore) {
    }
  }

  public void pass2() {
    try {
      Class<?> c = Class.forName("ReflectionConstructorTest");
      Constructor<?> init = c.getConstructor(new Class<?>[] {Integer.class, Integer.class});
      @TestReflectSibling2 int a = sibling2;
      int b = a;
      @TestReflectTop Object inst = init.newInstance(a, b);
    } catch (Exception ignore) {
    }
  }

  public void fail1() {
    try {
      Class<?> c = ReflectionConstructorTest.class;
      Constructor<?> init = c.getConstructor(new Class<?>[] {Integer.class});
      // :: error: (argument)
      Object o = init.newInstance(sibling2);
    } catch (Exception ignore) {
    }
  }

  public void fail2() {
    try {
      Class<?> c = ReflectionConstructorTest.class;
      Constructor<?> init = c.getConstructor(new Class<?>[] {Integer.class});
      // :: error: (argument) :: error: (assignment)
      @TestReflectSibling1 Object o = init.newInstance(new Object[] {sibling2});
    } catch (Exception ignore) {
    }
  }

  public void fail3() {
    try {
      Class<?> c = Class.forName("ReflectionConstructorTest");
      Constructor<?> init = c.getConstructor(new Class<?>[] {Integer.class, Integer.class});
      @TestReflectSibling2 int a = sibling2;
      @TestReflectSibling1 int b = sibling1;
      // :: error: (argument)
      @TestReflectSibling2 Object inst = init.newInstance(a, b);
    } catch (Exception ignore) {
    }
  }
}
