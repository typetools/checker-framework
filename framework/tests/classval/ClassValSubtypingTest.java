import org.checkerframework.common.reflection.qual.ClassBound;
import org.checkerframework.common.reflection.qual.ClassVal;

public class ClassValSubtypingTest {
  @ClassVal("a") Object a = null;

  @ClassVal({"a", "b"}) Object ab = null;

  @ClassVal("c") Object c = null;

  @ClassVal({"c", "d"}) Object cd = null;

  Object unknown = null;

  void assignToUnknown() {
    unknown = a;
    unknown = ab;
    unknown = c;
    unknown = cd;
  }

  void assignUnknown() {
    // :: error: (assignment.type.incompatible)
    a = unknown;
    // :: error: (assignment.type.incompatible)
    ab = unknown;
    // :: error: (assignment.type.incompatible)
    c = unknown;
    // :: error: (assignment.type.incompatible)
    cd = unknown;
  }

  void assignments() {
    // :: error: (assignment.type.incompatible)
    a = ab;
    ab = a;
    // :: error: (assignment.type.incompatible)
    a = c;
    // :: error: (assignment.type.incompatible)
    ab = c;
    // :: error: (assignment.type.incompatible)
    ab = cd;
  }
}

class ClassBoundSubtypingTest {
  @ClassBound("a") Object a = null;

  @ClassBound({"a", "b"}) Object ab = null;

  @ClassBound("c") Object c = null;

  @ClassBound({"c", "d"}) Object cd = null;

  Object unknown = null;

  void assignToUnknown() {
    unknown = a;
    unknown = ab;
    unknown = c;
    unknown = cd;
  }

  void assignUnknown() {
    // :: error: (assignment.type.incompatible)
    a = unknown;
    // :: error: (assignment.type.incompatible)
    ab = unknown;
    // :: error: (assignment.type.incompatible)
    c = unknown;
    // :: error: (assignment.type.incompatible)
    cd = unknown;
  }

  void assignments() {
    // :: error: (assignment.type.incompatible)
    a = ab;
    ab = a;
    // :: error: (assignment.type.incompatible)
    a = c;
    // :: error: (assignment.type.incompatible)
    ab = c;
    // :: error: (assignment.type.incompatible)
    ab = cd;
  }
}

class ClassValClassBoundSubtypingTest {
  @ClassVal("a") Object a = null;

  @ClassVal({"a", "b"}) Object ab = null;

  @ClassBound("a") Object aBound = null;

  @ClassBound({"a", "b"}) Object abBound = null;

  void assignments1() {
    // :: error: (assignment.type.incompatible)
    a = aBound;
    // :: error: (assignment.type.incompatible)
    ab = aBound;
    // :: error: (assignment.type.incompatible)
    a = abBound;
    // :: error: (assignment.type.incompatible)
    ab = abBound;
  }

  void assignments2() {
    aBound = a;
    // :: error: (assignment.type.incompatible)
    aBound = ab;

    abBound = a;
    abBound = ab;
  }
}
