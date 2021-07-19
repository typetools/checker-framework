import org.checkerframework.checker.nullness.qual.*;

public class AnnotatedGenerics3 {
    class Cell<T extends @Nullable Object> {
        T f;

        Cell(T i) {
            f = i;
        }

        void setNull(Cell<@Nullable T> p) {
            p.f = null;
        }

        void indirect(Cell<T> p) {
            // :: error: (argument.type.incompatible)
            setNull(p);
        }

        void setField(@Nullable T p) {
            // :: error: (assignment.type.incompatible)
            this.f = p;
        }
    }

    void run() {
        Cell<@NonNull Object> c = new Cell<>(new Object());
        // :: error: (argument.type.incompatible)
        c.setNull(c);
        c.f.hashCode();

        c.indirect(c);
        c.f.hashCode();

        c.setField(null);
        c.f.hashCode();
    }

    public static void main(String[] args) {
        new AnnotatedGenerics3().run();
    }
}
