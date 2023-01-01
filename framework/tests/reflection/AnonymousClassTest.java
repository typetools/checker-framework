import java.lang.reflect.Method;
import org.checkerframework.framework.testchecker.reflection.qual.TestReflectBottom;
import org.checkerframework.framework.testchecker.reflection.qual.TestReflectSibling1;
import org.checkerframework.framework.testchecker.reflection.qual.TestReflectSibling2;
import org.checkerframework.framework.testchecker.reflection.qual.TestReflectTop;

public class AnonymousClassTest {
  /**
   * To build/run outside of the JUnit tests:
   *
   * <p>Build with $CHECKERFRAMEWOKR/framework/tests/build/ on the classpath. Need to either use
   * Java 8 or the langtools compiler, because annotations on cast are used.
   *
   * <p>java AnonymousClassTest MyClass$1.getSib1() MyClass$1.setSib1() MyClass$1.setSib1()
   * MyClass$1.setSib2() MyClass$1.setSib2() MyClass$1.getSib2()
   */
  public static void main(String[] args) {
    AnonymousClassTest act = new AnonymousClassTest();
    act.returnTypePass();
    act.argumentTypePass();
    act.argumentTypeFail();
    act.returnTypeFail();
  }

  @TestReflectSibling1 int sibling1;
  @TestReflectSibling2 int sibling2;

  public void returnTypePass() {
    try {
      Class<?> c = Class.forName("AnonymousClassTest$1");
      Method m = c.getMethod("getSib1", new Class[] {});
      // TODO: Can we resolve anonymous classes?
      // :: error: (assignment)
      @TestReflectSibling1 Object a = m.invoke(anonymous, (@TestReflectBottom Object[]) null);
    } catch (Exception ignore) {
      ignore.printStackTrace();
    }
  }

  public void argumentTypePass() {
    String str = "setSib1";
    @TestReflectSibling1 int val1 = sibling1;
    @TestReflectSibling1 Integer val2 = val1;
    try {
      Class<?> c = Class.forName("AnonymousClassTest$1");
      Method m = c.getMethod(str, new Class[] {int.class});
      // TODO: Can we resolve anonymous classes?
      // :: error: (argument)
      m.invoke(anonymous, val1);
      // TODO: Can we resolve anonymous classes?
      // :: error: (argument)
      m.invoke(anonymous, val2);
    } catch (Exception ignore) {
      ignore.printStackTrace();
    }
  }

  public void argumentTypeFail() {
    String str = "setSib2";
    @TestReflectSibling1 int val1 = sibling1;
    @TestReflectSibling1 Integer val2 = val1;
    try {
      Class<?> c = Class.forName("AnonymousClassTest$1");
      Method m = c.getMethod(str, new Class[] {int.class});
      // :: error: (argument)
      m.invoke(anonymous, val1);
      // :: error: (argument)
      m.invoke(anonymous, val2);
    } catch (Exception ignore) {
      ignore.printStackTrace();
    }
  }

  public void returnTypeFail() {
    try {
      Class<?> c = Class.forName("AnonymousClassTest$1");
      Method m = c.getMethod("getSib2", new Class[] {});
      // :: error: (assignment)
      @TestReflectSibling1 Object a = m.invoke(anonymous, (@TestReflectBottom Object[]) null);
    } catch (Exception ignore) {
      ignore.printStackTrace();
    }
  }

  public @TestReflectBottom MyClass anonymous =
      // :: warning: (cast.unsafe.constructor.invocation)
      new @TestReflectBottom MyClass() {

        public @TestReflectSibling1 int getSib1() {
          System.out.println("MyClass$1.getSib1()");
          return 1;
        }

        public @TestReflectSibling2 int getSib2() {
          System.out.println("MyClass$1.getSib2()");
          return 1;
        }

        public void setSib1(@TestReflectSibling1 int a) {
          System.out.println("MyClass$1.setSib1()");
        }

        public void setSib2(@TestReflectSibling2 int a) {
          System.out.println("MyClass$1.setSib2()");
        }
      };

  class MyClass {

    public @TestReflectTop int getSib1() {
      System.out.println("MyClass.getSib1()");
      return 1;
    }

    public @TestReflectTop int getSib2() {
      System.out.println("MyClass.getSib1()");
      return 1;
    }

    public void setSib1(@TestReflectBottom int a) {
      System.out.println("MyClass.setSib1()");
    }

    public void setSib2(@TestReflectBottom int a) {
      System.out.println("MyClass.setSib2()");
    }
  }
}
