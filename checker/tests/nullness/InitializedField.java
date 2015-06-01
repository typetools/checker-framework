// @skip-test Failing test case, commented out to avoid breaking the build

import java.util.Stack;

import org.checkerframework.dataflow.qual.*;
import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public final class InitializedField {
  private Stack<Object> stack;

  InitializedField() {
    stack = new Stack<Object>();
    iPeek();
  }

  /*@RequiresNonNull("stack")*/
  public Object iPeek(/*>>>@UnknownInitialization @Raw InitializedField this*/) {
    return stack.peek();
  }

  public static void testJavaClass (InitializedField initField) {
    initField.iPeek();
  }

}
