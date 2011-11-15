import checkers.nullness.quals.*;

class Ternary {
    class Generic1<T extends @NonNull Object> {
        void cond(boolean b, T p) {
            //:: error: (assignment.type.incompatible)
            T r1 = b ? p : null;
            //:: error: (assignment.type.incompatible)
            T r2 = b ? null : p;
        }
    }

    class Generic2<T extends @Nullable Object> {
        void cond(boolean b, T p) {
            T r1 = b ? p : null;
            T r2 = b ? null : p;
        }
    }

    class Generic3<T> {
        void cond(boolean b, @Nullable T p) {
            @Nullable T r1 = b ? p : null;
            @Nullable T r2 = b ? null : p;
            //:: error: (assignment.type.incompatible)
            T r3 = b ? null : p;
        }
    }

    void m4(boolean b) {
        // TODO: flow inference for array components doesn't seem to work!
        // Without the explicit @Nullable we get an assignment incompatible error.
        @Nullable String[] s = b ? new String[5] : null;
        //:: error: (dereference.of.nullable)
        s.toString();
    }
}
