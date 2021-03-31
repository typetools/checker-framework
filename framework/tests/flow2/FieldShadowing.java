import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.qual.RequiresQualifier;
import org.checkerframework.framework.test.*;
import org.checkerframework.framework.testchecker.util.*;

// various tests for the precondition mechanism
public class FieldShadowing {

  String f;

  class Sub extends FieldShadowing {
    String f;

    @Pure
    @RequiresQualifier(expression = "f", qualifier = Odd.class)
    int reqSub() {
      @Odd String l2 = f;
      // :: error: (assignment.type.incompatible)
      @Odd String l1 = super.f;
      int i;
      i = 1;
      return 1;
    }

    @Pure
    @RequiresQualifier(expression = "super.f", qualifier = Odd.class)
    int reqSuper() {
      // :: error: (assignment.type.incompatible)
      @Odd String l2 = f;
      @Odd String l1 = super.f;
      return 1;
    }

    void t1(@Odd String p1) {
      f = p1;
      // :: error: (contracts.precondition.not.satisfied)
      reqSuper();
      reqSub();
    }

    void t2(@Odd String p1) {
      super.f = p1;
      // :: error: (contracts.precondition.not.satisfied)
      reqSub();
      reqSuper();
    }
  }
}
