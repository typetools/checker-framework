import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import org.checkerframework.checker.regex.qual.*;

public class RawTypeTest {

  public void m1(Class<?> c) {
    Class<? extends I2> x = c.asSubclass(I2.class);

    new WeakReference<Object>(x);
    new WeakReference<Class>(x);
    new WeakReference<Class<? extends I2>>(x);

    new WeakReference<Object>(c.asSubclass(I2.class));
    new WeakReference<Class>(c.asSubclass(I2.class));
    new WeakReference<Class<? extends I2>>(c.asSubclass(I2.class));
  }

  /* It would be desirable to optionally check the following code without
   * warnings. See issue 119:
   *
   * https://github.com/typetools/checker-framework/issues/119
   *
  class Raw {
      public void m2(Class<Object> c) {}

      public void m3(Class c) {
          m2(c);
      }

      public void m4() {
          AccessController.doPrivileged(new PrivilegedAction() {
              public Object run() {
                  return null;
              }});
      }

      public void m5(List list, C4 c) {
          list.add(c);
      }

      public void m6(List list, long l) {
          list.add(l);
      }
  }*/

  class NonRaw {
    public void m2(Class<Object> c) {}

    public void m3(Class<Object> c) {
      m2(c);
    }

    @SuppressWarnings("removal") // AccessController is deprecated for removal in Java 17
    public void m4() {
      AccessController.doPrivileged(
          new PrivilegedAction<Object>() {
            public Object run() {
              return null;
            }
          });
    }

    public void m5(List<C4> list, C4 c) {
      list.add(c);
    }

    public void m6(List<Long> list, long l) {
      list.add(l);
    }
  }

  class MyList<X extends @Regex String> {
    X f;
  }

  interface I1 {
    public void m(MyList<? extends @Regex String> l);
  }

  class C1 implements I1 {
    public void m(MyList par) {
      @Regex String xxx = par.f;
    }
  }

  interface I2 {
    public void m(MyList<@Regex String> l);
  }

  class C2 implements I2 {
    public void m(MyList<@Regex String> l) {}
  }

  class C3 implements I2 {
    // :: error: (override.param) :: error: (type.argument)
    public void m(MyList<String> l) {}
  }

  class C4 {}
}
