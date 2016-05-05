
import tests.reflection.qual.Top;
import tests.reflection.qual.Sibling1;
import tests.reflection.qual.Sibling2;

import java.lang.reflect.Constructor;

class ConstructorTest {
    @Sibling1 int sibling1;
    @Sibling2 int sibling2;
    public @Sibling1 ConstructorTest(@Sibling1 int a) {
    }

    public @Sibling2 ConstructorTest(@Sibling2 int a, @Sibling2 int b) {
    }

    public void pass1() {
        try {
            Class<?> c = Class.forName("ConstructorTest");
            Constructor init = c
                    .getConstructor(new Class<?>[] { Integer.class });
            @Sibling1 int i = sibling1;
            @Sibling1 Object o = init.newInstance(i);
        } catch (Exception ignore) {
        }
    }

    public void pass2() {
        try {
            Class<?> c = Class.forName("ConstructorTest");
            Constructor init = c.getConstructor(new Class<?>[] { Integer.class,
                    Integer.class });
            @Sibling2 int a = sibling2;
            int b = a;
            @Top Object inst = init.newInstance(a, b);
        } catch (Exception ignore) {
        }
    }

    public void fail1() {
        try {
            Class<?> c = ConstructorTest.class;
            Constructor init = c
                    .getConstructor(new Class<?>[] { Integer.class });
            //:: error: (argument.type.incompatible)
            Object o = init.newInstance(sibling2);
        } catch (Exception ignore) {
        }
    }

    public void fail2() {
        try {
            Class<?> c = ConstructorTest.class;
            Constructor init = c
                    .getConstructor(new Class<?>[] { Integer.class });
            // The error should be//:: error: (argument.type.incompatible)s
            //:: error: (assignment.type.incompatible)
            @Sibling1 Object o = init.newInstance(new Object[] { sibling2 });
        } catch (Exception ignore) {
        }
    }

    public void fail3() {
        try {
            Class<?> c = Class.forName("ConstructorTest");
            Constructor init = c.getConstructor(new Class<?>[] { Integer.class,
                    Integer.class });
            @Sibling2 int a = sibling2;
            @Sibling1 int b = sibling1;
            //:: error: (argument.type.incompatible)
            @Sibling2 Object inst = init.newInstance(a, b);
        } catch (Exception ignore) {
        }
    }
}
