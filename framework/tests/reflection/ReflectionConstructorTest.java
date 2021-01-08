import java.lang.reflect.Constructor;
import org.checkerframework.framework.testchecker.reflection.qual.Sibling1;
import org.checkerframework.framework.testchecker.reflection.qual.Sibling2;
import org.checkerframework.framework.testchecker.reflection.qual.Top;

public class ReflectionConstructorTest {
    @Sibling1 int sibling1;
    @Sibling2 int sibling2;

    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    public @Sibling1 ReflectionConstructorTest(@Sibling1 int a) {}

    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    public @Sibling2 ReflectionConstructorTest(@Sibling2 int a, @Sibling2 int b) {}

    public void pass1() {
        try {
            Class<?> c = Class.forName("ReflectionConstructorTest");
            Constructor<?> init = c.getConstructor(new Class<?>[] {Integer.class});
            @Sibling1 int i = sibling1;
            @Sibling1 Object o = init.newInstance(i);
        } catch (Exception ignore) {
        }
    }

    public void pass2() {
        try {
            Class<?> c = Class.forName("ReflectionConstructorTest");
            Constructor<?> init = c.getConstructor(new Class<?>[] {Integer.class, Integer.class});
            @Sibling2 int a = sibling2;
            int b = a;
            @Top Object inst = init.newInstance(a, b);
        } catch (Exception ignore) {
        }
    }

    public void fail1() {
        try {
            Class<?> c = ReflectionConstructorTest.class;
            Constructor<?> init = c.getConstructor(new Class<?>[] {Integer.class});
            // :: error: (argument.type.incompatible)
            Object o = init.newInstance(sibling2);
        } catch (Exception ignore) {
        }
    }

    public void fail2() {
        try {
            Class<?> c = ReflectionConstructorTest.class;
            Constructor<?> init = c.getConstructor(new Class<?>[] {Integer.class});
            // :: error: (assignment.type.incompatible)
            @Sibling1 Object o = init.newInstance(new Object[] {sibling2});
        } catch (Exception ignore) {
        }
    }

    public void fail3() {
        try {
            Class<?> c = Class.forName("ReflectionConstructorTest");
            Constructor<?> init = c.getConstructor(new Class<?>[] {Integer.class, Integer.class});
            @Sibling2 int a = sibling2;
            @Sibling1 int b = sibling1;
            // :: error: (argument.type.incompatible)
            @Sibling2 Object inst = init.newInstance(a, b);
        } catch (Exception ignore) {
        }
    }
}
