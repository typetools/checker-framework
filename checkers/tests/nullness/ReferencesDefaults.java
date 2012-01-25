public class ReferencesDefaults {
    @checkers.quals.DefaultQualifier("checkers.nullness.quals.Nullable")
    class Decl {
        Object test() {
            // legal, because of changed default.
            return null;
        }
    }

    class Use {
        Decl d = new Decl();
        // here the default for f is NonNull -> error
        //:: error: (assignment.type.incompatible)
        Object f = d.test();
    }
}