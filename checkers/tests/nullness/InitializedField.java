// @skip-test Failing test case, commented out to avoid breaking the build

import java.util.Stack;

import dataflow.quals.*;
import checkers.initialization.quals.*;
import checkers.nullness.quals.*;

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
