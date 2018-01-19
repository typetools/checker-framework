
import org.checkerframework.checker.nullness.qual.Nullable;

public class ClassTypeArgInference {
    public static void main(String[] args) {
        Gen<String> o = new Gen<>("");
        // :: error: (methodref.param.invalid)
        Factory f = Gen::make;
        // :: error: (methodref.param.invalid)
        Factory f2 = Gen<String>::make;
        // :: error: (methodref.receiver.invalid) :: error: (methodref.return.invalid)
        Factory f3 = Gen<@Nullable String>::make;
        f2.make(o, null).toString();

        /*  Factory2 factory2a = Gen<@Nullable String>::new;
        Factory2 factory2b = Gen<String>::new;
        Factory2 factory2c = Gen::new; */

        //factory2a.create(null).getField().toString();

    }

    static class Gen<G> {
        G field;

        Gen(G g) {
            field = g;
        }

        public G getField() {
            return field;
        }

        G make(G g) {
            return g;
        }

        Gen<G> id() {
            return this;
        }
    }

    interface Factory {
        String make(Gen<String> g, @Nullable String t);
    }

    interface Factory2 {
        Gen<String> create(@Nullable String s);
    }
}
