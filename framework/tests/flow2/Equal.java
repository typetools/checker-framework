import org.checkerframework.framework.testchecker.util.*;

public class Equal {

  String f1, f2, f3;

  // annotation inference for equality
  void t1(@Odd String p1, String p2) {
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = f1;
    if (f1 == p1) {
      @Odd String l2 = f1;
    } else {
      // :: error: (assignment.type.incompatible)
      @Odd String l3 = f1;
    }
  }

  // annotation inference for equality
  void t2(@Odd String p1, String p2) {
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = f1;
    if (f1 != p1) {
      // :: error: (assignment.type.incompatible)
      @Odd String l2 = f1;
    } else {
      @Odd String l3 = f1;
    }
  }

  void t3(@Odd Long p1) {
    // :: error: (assignment.type.incompatible)
    @Odd Long l1 = Long.valueOf(2L);
    if (Long.valueOf(2L) == p1) {
      // The result of valueOf is not deterministic, so it can't be refined.
      // :: error: (assignment.type.incompatible)
      @Odd Long l2 = Long.valueOf(2L);
    } else {
      // :: error: (assignment.type.incompatible)
      @Odd Long l3 = Long.valueOf(2L);
    }
  }
}
