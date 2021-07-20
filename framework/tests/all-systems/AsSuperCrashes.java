package assuper;

import org.checkerframework.dataflow.qual.Pure;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

// This class has code that used to cause AsSuper to crash
@SuppressWarnings("all")
public class AsSuperCrashes {
    // TODO: Value Checker crashes on this
    /*    void primitiveNarrowing() {
            Byte b = 100;
            Character c = 100;
            Short s = 100;

            byte bb = 100;
            char cc = 100;
            short ss = 100;
        }
    */

    // test anonymous classes
    private void testAnonymous() {
        new Object() {
            public boolean equals(Object o) {
                return true;
            }
        }.equals(null);

        Date d = new Date() {};
    }

    private void apply(Field field) {
        Class<?> type = field.getType();
        type.getSuperclass().getName().equals("java.lang.Enum");
    }

    void arrayAsMethodReceiver(Object[] array) {
        array.clone();
    }

    <T> T lowerBoundedWildcard(java.util.List<? super Iterable<?>> l) {
        lowerBoundedWildcard(new java.util.ArrayList<Object>());
        throw new Error();
    }

    // Test super() and this()
    class Inner {
        public Inner() {
            super();
        }

        public Inner(int i) {
            this();
        }
    }

    public static <T extends Interface<? super T>> void foo2(T a, T b) {
        a.compareTo(b);
    }

    public static <T extends Object & Interface<? super T>> void foo(T a, T b) {
        a.compareTo(b);
    }

    interface Interface<F> {
        void compareTo(F t);
    }

    public void m1(Class<?> c) {
        Class<? extends I2> x = c.asSubclass(I2.class);
        new WeakReference<Class<? extends I2>>(c.asSubclass(I2.class));
    }

    interface I2 {}

    @Pure
    void bar() {
        bar();
    }

    public static <Z> void copy(List<? super Z> dest, List<? extends Z> src) {
        dest.set(0, src.get(0));
    }

    public static <F, E extends F> void copy2(List<? super F> dest, List<E> src) {
        dest.set(0, src.get(0));
    }
}
