import java.lang.reflect.Method;
import org.checkerframework.framework.testchecker.reflection.qual.ReflectBottom;
import org.checkerframework.framework.testchecker.reflection.qual.Sibling1;
import org.checkerframework.framework.testchecker.reflection.qual.Sibling2;
import org.checkerframework.framework.testchecker.reflection.qual.Top;

public class MethodTest {

  @Sibling1 int sibling1;
  @Sibling2 int sibling2;
  @ReflectBottom SuperClass superClass;

  public void real_class() {
    try {
      Class<?> c = Object.class;
      Method m = c.getMethod("equals", Object.class);
      Object rec = new Object();
      Object param = new Object();
      Boolean other = (Boolean) rec.equals(param);
      Boolean equals = (Boolean) m.invoke(rec, param);
    } catch (Exception ignore) {
    }
  }

  public void pass1() {
    try {
      Class<?> c = Class.forName("MethodTest$SuperClass");
      Method m = c.getMethod("getA", new Class[] {});
      @Sibling1 Object a = m.invoke(superClass, (@ReflectBottom Object[]) null);
    } catch (Exception ignore) {
    }
  }

  public void pass1b() {
    try {
      Class<?> c = Class.forName("MethodTest$SuperClass");
      Method m = c.getMethod("getA", (Class[]) null);
      @Sibling1 Object a = m.invoke(superClass, (@ReflectBottom Object[]) null);
    } catch (Exception ignore) {
    }
  }

  public void pass2() {
    String str = "get" + "A";
    try {
      Class<?> c = Class.forName("MethodTest$SuperClass");
      Method m = c.getMethod(str, new Class[] {});
      @Sibling1 Object a = m.invoke(superClass, (@ReflectBottom Object[]) null);
    } catch (Exception ignore) {
    }
  }

  public void pass3() {
    String str = "get";
    str += "A";
    try {
      Class<?> c = Class.forName("MethodTest$SuperClass");
      Method m = c.getMethod(str, new Class[] {});
      // TODO: Should not fail -> enhance Value checker
      // and remove the expected error

      // :: error: (assignment)
      @Sibling1 Object a = m.invoke(superClass, (@ReflectBottom Object[]) null);
    } catch (Exception ignore) {
    }
  }

  public void pass4() {
    String str = "setA";
    @Sibling1 int val1 = sibling1;
    @Sibling1 Integer val2 = val1;
    try {
      Class<?> c = Class.forName("MethodTest$SuperClass");
      Method m = c.getMethod(str, new Class[] {Integer.class});
      m.invoke(superClass, val1);
      m.invoke(superClass, val2);
    } catch (Exception ignore) {
    }
  }

  public void pass4b() {
    String str = "setA";
    @Sibling1 int val1 = sibling1;
    @Sibling1 Integer val2 = val1;
    try {
      //
      Class<?> c = Class.forName("MethodTest$SuperClass");
      Method m = c.getMethod(str, int.class);
      m.invoke(superClass, val1);
      m.invoke(superClass, val2);
    } catch (Exception ignore) {
    }
  }

  @ReflectBottom SubClass subClass;
  // Test resolution of methods declared in super class
  public void pass5() {
    try {
      Class<?> c = Class.forName("MethodTest$SubClass");
      Method m = c.getMethod("getB", new Class[0]);
      @Sibling2 Object o = m.invoke(subClass, (@ReflectBottom Object[]) null);
    } catch (Exception ignore) {
    }
  }

  // Test resolution of static methods
  public void pass6() {
    try {
      Class<?> c = MethodTest.class;
      Method m = c.getMethod("convertSibling2ToSibling1", new Class[] {Integer.class});
      @Sibling1 Object o = m.invoke(null, sibling2);
    } catch (Exception ignore) {
    }
  }

  // Test primitives
  public void pass7() {
    try {
      Class<?> c = MethodTest.class;
      Method m = c.getMethod("convertSibling2ToSibling1", new Class[] {int.class});
      @Sibling1 Object o = m.invoke(null, sibling2);
    } catch (Exception ignore) {
    }
  }

  public void pass8() {
    String str = "setA";
    try {
      Class<?> c = Class.forName("MethodTest$SuperClass");
      Method m = c.getMethod(str, new Class[] {Integer.class});
      m.invoke(superClass, sibling1);
    } catch (Exception ignore) {
    }
  }

  public void pass9() {
    String str = "getA";
    if (true) {
      str = "getB";
    }
    try {
      Class<?> c = Class.forName("MethodTest$SubClass");
      Method m = c.getMethod(str, new Class[0]);
      @Top Object o = m.invoke(subClass, (@ReflectBottom Object[]) null);
    } catch (Exception ignore) {
    }
  }

  // Test getClass()
  public void pass10() {
    SuperClass inst = new SubClass();
    try {
      Class<?> c = inst.getClass();
      Method m = c.getMethod("getA", new Class[0]);
      @Sibling1 Object o = m.invoke(inst, (@ReflectBottom Object[]) null);
    } catch (Exception ignore) {
    }
  }

  public void pass11() {
    try {
      Class<?> c = this.getClass();
      Method m = c.getMethod("convertSibling2ToSibling1", new Class[] {Integer.class});
      @Sibling1 Object o = m.invoke(null, sibling2);
    } catch (Exception ignore) {
    }
  }

  public void pass11b() {
    try {
      Class<?> c = getClass();
      Method m = c.getMethod("convertSibling2ToSibling1", new Class[] {Integer.class});
      @Sibling1 Object o = m.invoke(null, sibling2);
    } catch (Exception ignore) {
    }
  }

  // Test .class on inner class
  public void pass12() {
    try {
      Class<?> c = SuperClass.class;
      Method m = c.getMethod("getA", new Class[0]);
      @Sibling1 Object o = m.invoke(new SuperClass(), new @ReflectBottom Object @ReflectBottom [0]);
    } catch (Exception ignore) {
    }
  }

  boolean flag = false;
  // Test lub of return types
  public void testLubReturnPass() {
    try {
      Class<?> c = Class.forName("MethodTest$SuperClass");
      Method m;
      if (flag) {
        m = c.getMethod("getA", new Class[0]);
      } else {
        m = c.getMethod("getB", new Class[0]);
      }
      @Top Object o = m.invoke(new SuperClass(), new @ReflectBottom Object @ReflectBottom [0]);
    } catch (Exception ignore) {
    }
  }

  public void testLubReturnFail() {
    try {
      Class<?> c = Class.forName("MethodTest$SuperClass");
      Method m;
      if (flag) {
        m = c.getMethod("getA", new Class[0]);
      } else {
        m = c.getMethod("getB", new Class[0]);
      }
      // :: error: (assignment)
      @ReflectBottom Object o = m.invoke(new SuperClass(), new @ReflectBottom Object @ReflectBottom [0]);
    } catch (Exception ignore) {
    }
  }

  public void test() {}

  public void fail1() {
    try {
      Class<?> c = MethodTest.class;
      Method m = c.getMethod("convertSibling2ToSibling1", new Class[] {Integer.class});
      // :: error: (argument)
      Object o = m.invoke(null, sibling1);
    } catch (Exception ignore) {
    }
  }

  // Test unresolvable methods
  public void fail2(String str) {
    try {
      Class<?> c = Class.forName(str);
      Method m = c.getMethod("getA", new Class[] {Integer.class});
      // :: error: (assignment)
      @Sibling1 Object o = m.invoke(subClass, (@ReflectBottom Object[]) null);
    } catch (Exception ignore) {
    }
  }

  public void fail3() {
    String str = "setB";
    try {
      Class<?> c = Class.forName("MethodTest$SuperClass");
      Method m = c.getMethod(str, new Class[] {Integer.class});
      // :: error: (argument)
      m.invoke(this, sibling1);
    } catch (Exception ignore) {
    }
  }

  public void fail4() {
    String str = "setA";
    try {
      Class<?> c = Class.forName("MethodTest$SubClass");
      Method m = c.getMethod(str, new Class[] {Integer.class});
      // :: error: (argument)
      m.invoke(this, new Object[] {sibling2});
    } catch (Exception ignore) {
    }
  }

  public void fail5() {
    String str = "setAB";
    try {
      Class<?> c = Class.forName("MethodTest$SubClass");
      Method m = c.getMethod(str, new Class[] {Integer.class, Integer.class});
      // :: error: (argument)
      m.invoke(this, new Object[] {sibling1, sibling2});
    } catch (Exception ignore) {
    }
  }

  public void fail6() {
    String str = "setA";
    if (true) {
      str = "setB";
    }
    try {
      Class<?> c = Class.forName("MethodTest$SubClass");
      Method m = c.getMethod(str, new Class[] {Integer.class});
      // :: error: (argument)
      m.invoke(this, new Object[] {sibling1});
    } catch (Exception ignore) {
    }
  }

  public void fail7() {
    // :: warning: (cast.unsafe.constructor.invocation)
    @Sibling2 MethodTest inst = new @Sibling2 MethodTest();
    try {
      Class<?> c = MethodTest.class;
      Method m = c.getMethod("convertSibling2ToSibling1", new Class[] {Integer.class});
      @Sibling1 Object o = m.invoke(inst, sibling2);
    } catch (Exception ignore) {
    }
  }

  // Test method call that cannot be uniquely resolved
  public void fail8() {
    try {
      Class<?> c = SuperClass.class;
      Method m = c.getMethod("setC", new Class[] {Integer.class});
      // :: error: (argument)
      Object o = m.invoke(new SuperClass(), new Object[] {sibling2});
    } catch (Exception ignore) {
    }
  }

  public void bug() {
    String str = "setA";
    @Sibling1 int val1 = sibling1;
    @Sibling1 Object[] args = new Object[] {val1};
    try {
      //
      Class<?> c = Class.forName("MethodTest$SuperClass");

      Method m = c.getMethod(str, int.class);
      // This error is a bug.
      // See DefaultReflectionResolver.resolveMethodCall(...)
      // for details.
      // :: error: (argument)
      m.invoke(this, args);
    } catch (Exception ignore) {
    }
  }

  public void bug2() {
    String str = "setAB";
    @Sibling1 int val1 = sibling1;
    @Sibling2 int val2 = sibling2;

    Object[] args = new Object[] {val1, val2};
    try {
      //
      Class<?> c = Class.forName("MethodTest$SuperClass");
      Method m = c.getMethod(str, int.class, int.class);
      // This error is a bug.
      // See DefaultReflectionResolver.resolveMethodCall(...)
      // for details.
      // :: error: (argument)
      m.invoke(this, args);
    } catch (Exception ignore) {
    }
  }

  public static @Sibling1 int convertSibling2ToSibling1(@Sibling2 int a) {
    return (@Sibling1 int) 1;
  }

  // TODO: Does the testing framework somehow support the compilation of
  // multiple files at the same time?
  private class SubClass extends SuperClass {}

  private class SuperClass {
    private @Sibling1 int a;
    private @Sibling2 int b;
    private @Sibling1 Integer c;

    public SuperClass() {
      this.a = sibling1;
      this.b = sibling2;
    }

    public @Sibling1 int getA() {
      return a;
    }

    public void setA(@Sibling1 int a) {
      this.a = a;
    }

    public @Sibling2 int getB() {
      return b;
    }

    public void setB(@Sibling2 int b) {
      this.b = b;
    }

    public void setAB(@Sibling1 int a, @Sibling2 int b) {
      this.a = a;
      this.b = b;
    }

    public void setC(@Sibling1 int c) {
      this.c = c;
    }

    public void setC(@Sibling1 Integer c) {
      this.c = c;
    }
  }
}
