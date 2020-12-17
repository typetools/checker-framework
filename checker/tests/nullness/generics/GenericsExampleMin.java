import org.checkerframework.checker.nullness.qual.*;

// This is the example from manual section:
// "Generics (parametric polymorphism or type polymorphism)"
// whose source code is ../../../docs/manual/advanced-features.tex
public class GenericsExampleMin {

    class MyList1<@Nullable T> {
        T t;
        @Nullable T nble;
        @NonNull T nn;

        public MyList1(T t, @Nullable T nble, @NonNull T nn) {
            this.t = t;
            this.nble = nble;
            this.nn = nn;
            this.t = this.nble;
        }

        T get(int i) {
            return t;
        }

        // This method works.
        // Note that it fails to work if it is moved after m2() in the syntax tree.
        // TODO: the above comment seems out-of-date, as method
        // m3 below works.
        void m1() {
            t = this.get(0);
            nble = this.get(0);
        }

        // When the assignment to nn is added, the assignments to t and nble also fail, which is
        // unexpected.
        void m2() {
            // :: error: (assignment.type.incompatible)
            nn = null;
            t = this.get(0);
            nble = this.get(0);
        }

        void m3() {
            t = this.get(0);
            nble = this.get(0);
        }
    }

    class MyList2<@NonNull T> {
        T t;
        @Nullable T nble;

        public MyList2(T t, @Nullable T nble) {
            // :: error: (assignment.type.incompatible)
            this.t = this.nble; // error
            // :: error: (assignment.type.incompatible)
            this.t = nble; // error
        }
    }

    class MyList3<T extends @Nullable Object> {
        T t;
        @Nullable T nble;
        @NonNull T nn;

        public MyList3(T t, @Nullable T nble, @NonNull T nn) {
            // :: error: (assignment.type.incompatible)
            this.t = nble;
            this.t = nn;
            // :: error: (assignment.type.incompatible)
            this.nn = t;
            // :: error: (assignment.type.incompatible)
            this.nn = nble;
            this.nn = nn;
        }
    }

    class MyList4<T extends @NonNull Object> {
        T t;
        @Nullable T nble;
        @NonNull T nn;

        public MyList4(T t, @Nullable T nble, @NonNull T nn) {
            // :: error: (assignment.type.incompatible)
            this.t = nble;
            this.t = nn;
            this.nn = t;
            // :: error: (assignment.type.incompatible)
            this.nn = nble;
            this.nn = nn;
            this.nn = t;
            this.nble = t;
        }
    }
}
