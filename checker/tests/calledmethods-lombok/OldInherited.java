// This is a test for the old @ReturnsReceiver annotation, which is inherited.
// No one should ever write that annotation, but Lombok generates it as of version 1.18.10.

import org.checkerframework.checker.builder.qual.ReturnsReceiver;
import org.checkerframework.checker.calledmethods.qual.*;

public class OldInherited {
    @ReturnsReceiver
    OldInherited getThis() {
        return this;
    }

    static class OldInheritedChild extends OldInherited {
        @java.lang.Override
        OldInherited getThis() {
            return this;
        }
    }

    void requiresGetThis(@CalledMethods("getThis") OldInherited this) {}

    public static void testGoodParent() {
        OldInherited o = new OldInherited();
        o.getThis();
        o.requiresGetThis();
    }

    public static void testGoodChild() {
        OldInheritedChild o = new OldInheritedChild();
        o.getThis();
        o.requiresGetThis();
    }

    public static void testBadParent() {
        OldInherited o = new OldInherited();
        // :: error: finalizer.invocation.invalid
        o.requiresGetThis();
    }

    public static void testBadChild() {
        OldInheritedChild o = new OldInheritedChild();
        // :: error: finalizer.invocation.invalid
        o.requiresGetThis();
    }
}
