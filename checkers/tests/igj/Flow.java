import checkers.igj.quals.*;
import java.util.*;

public class Flow {
    <T> @Immutable List<T> emptyList() { return null; }

    public void testFlow() {

        //:: error: (assignment.type.incompatible)
        @Mutable List<String> m = emptyList();

        List<String> im = emptyList();
        //:: error: (method.invocation.invalid)
        im.add("m");
    }

    static void assertImmutable(@Immutable Object o) {}

    public void initializerEffect() {
        Object a = emptyList();
        assertImmutable(a); // valid

        // initializer shouldn't affect type if explicit
        @ReadOnly Object b = emptyList();
        //:: error: (argument.type.incompatible)
        assertImmutable(b);

        // assignments afterwards does affect it
        @ReadOnly Object c = null;
        c = emptyList();
        assertImmutable(c);
    }
}

