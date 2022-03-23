import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue5075NPE {

    static class C<V extends @Nullable Object> {
        void useBoxer(Boxer b) {
            // :: error: (assignment.type.incompatible)
            Box<? extends @NonNull Object> o = b.getBox();
            o.get().toString();
        }

        void useC(C<V> c) {}

        class Boxer {
            V v;

            Boxer(V in) {
                this.v = in;
            }

            Box<V> getBox() {
                return new Box<V>(v);
            }
        }
    }

    // Doesn't matter whether T's bound is explicit
    static class Box<T> {
        T f;

        Box(T p) {
            this.f = p;
        }

        T get() {
            return f;
        }
    }

    public static void main(String[] args) {
        C<@Nullable Issue5075NPE> c = new C<>();
        c.useBoxer(c.new Boxer(null));
    }
}
