import java.lang.reflect.Method;
import org.checkerframework.framework.testchecker.reflection.qual.ReflectBottom;
import org.checkerframework.framework.testchecker.reflection.qual.Sibling1;
import org.checkerframework.framework.testchecker.reflection.qual.Sibling2;
import org.checkerframework.framework.testchecker.reflection.qual.Top;

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

    @Sibling1 int sibling1;
    @Sibling2 int sibling2;

    public void returnTypePass() {
        try {
            Class<?> c = Class.forName("AnonymousClassTest$1");
            Method m = c.getMethod("getSib1", new Class[] {});
            // TODO: Can we resolve anonymous classes?
            // :: error: (assignment.type.incompatible)
            @Sibling1 Object a = m.invoke(anonymous, (@ReflectBottom Object[]) null);
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    public void argumentTypePass() {
        String str = "setSib1";
        @Sibling1 int val1 = sibling1;
        @Sibling1 Integer val2 = val1;
        try {
            Class<?> c = Class.forName("AnonymousClassTest$1");
            Method m = c.getMethod(str, new Class[] {int.class});
            // TODO: Can we resolve anonymous classes?
            // :: error: (argument.type.incompatible)
            m.invoke(anonymous, val1);
            // TODO: Can we resolve anonymous classes?
            // :: error: (argument.type.incompatible)
            m.invoke(anonymous, val2);
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    public void argumentTypeFail() {
        String str = "setSib2";
        @Sibling1 int val1 = sibling1;
        @Sibling1 Integer val2 = val1;
        try {
            Class<?> c = Class.forName("AnonymousClassTest$1");
            Method m = c.getMethod(str, new Class[] {int.class});
            // :: error: (argument.type.incompatible)
            m.invoke(anonymous, val1);
            // :: error: (argument.type.incompatible)
            m.invoke(anonymous, val2);
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    public void returnTypeFail() {
        try {
            Class<?> c = Class.forName("AnonymousClassTest$1");
            Method m = c.getMethod("getSib2", new Class[] {});
            // :: error: (assignment.type.incompatible)
            @Sibling1 Object a = m.invoke(anonymous, (@ReflectBottom Object[]) null);
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    public @ReflectBottom MyClass anonymous =
            // :: warning: (cast.unsafe.constructor.invocation)
            new @ReflectBottom MyClass() {

                public @Sibling1 int getSib1() {
                    System.out.println("MyClass$1.getSib1()");
                    return 1;
                }

                public @Sibling2 int getSib2() {
                    System.out.println("MyClass$1.getSib2()");
                    return 1;
                }

                public void setSib1(@Sibling1 int a) {
                    System.out.println("MyClass$1.setSib1()");
                }

                public void setSib2(@Sibling2 int a) {
                    System.out.println("MyClass$1.setSib2()");
                }
            };

    class MyClass {

        public @Top int getSib1() {
            System.out.println("MyClass.getSib1()");
            return 1;
        }

        public @Top int getSib2() {
            System.out.println("MyClass.getSib1()");
            return 1;
        }

        public void setSib1(@ReflectBottom int a) {
            System.out.println("MyClass.setSib1()");
        }

        public void setSib2(@ReflectBottom int a) {
            System.out.println("MyClass.setSib2()");
        }
    }
}
