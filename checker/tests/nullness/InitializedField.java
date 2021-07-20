import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;

import java.util.Stack;

public final class InitializedField {
    private Stack<Object> stack;

    InitializedField() {
        stack = new Stack<Object>();
        iPeek();
    }

    @RequiresNonNull("stack")
    public Object iPeek(@UnknownInitialization InitializedField this) {
        return stack.peek();
    }

    public static void testJavaClass(InitializedField initField) {
        initField.iPeek();
    }
}
