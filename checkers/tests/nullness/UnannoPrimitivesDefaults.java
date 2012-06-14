public class UnannoPrimitivesDefaults {
    @checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")
    class Decl {
        // The return type is not annotated with @NonNull, because
        // the implicit annotation for @Primitive takes precedence.
        int test() {
            return 5;
        }
    }

    class Use {
        Decl d = new Decl();
        int x = d.test();
    }
}