// Skip this test for now and re-write it using a framework type system
//@skip-test

import static org.checkerframework.checker.unit.UnitsTools.*;
import org.checkerframework.checker.units.qual.g;
import org.checkerframework.checker.units.qual.kg;
import org.checkerframework.checker.units.qual.Mass;

import java.lang.reflect.Constructor;

class ConstructorTest {

    public @g ConstructorTest(@g int a) {
    }

    public @kg ConstructorTest(@kg int a, @kg int b) {
    }

    public void pass1() {
        try {
            Class<?> c = Class.forName("ConstructorTest");
            Constructor init = c
                    .getConstructor(new Class<?>[] { Integer.class });
            @g int i = g;
            @g Object o = init.newInstance(i);
        } catch (Exception ignore) {
        }
    }

    public void pass2() {
        try {
            Class<?> c = Class.forName("ConstructorTest");
            Constructor init = c.getConstructor(new Class<?>[] { Integer.class,
                    Integer.class });
            @kg int a = kg;
            int b = a;
            @Mass Object inst = init.newInstance(a, b);
        } catch (Exception ignore) {
        }
    }

    public void fail1() {
        try {
            Class<?> c = ConstructorTest.class;
            Constructor init = c
                    .getConstructor(new Class<?>[] { Integer.class });
            //:: error: (argument.type.incompatible)
            Object o = init.newInstance(kg);
        } catch (Exception ignore) {
        }
    }

    public void fail2() {
        try {
            Class<?> c = ConstructorTest.class;
            Constructor init = c
                    .getConstructor(new Class<?>[] { Integer.class });
            //:: error: (argument.type.incompatible)
            @g Object o = init.newInstance(new Object[] { kg });
        } catch (Exception ignore) {
        }
    }

    public void fail3() {
        try {
            Class<?> c = Class.forName("ConstructorTest");
            Constructor init = c.getConstructor(new Class<?>[] { Integer.class,
                    Integer.class });
            @kg int a = kg;
            @g int b = g;
            //:: error: (argument.type.incompatible)
            @kg Object inst = init.newInstance(a, b);
        } catch (Exception ignore) {
        }
    }
}
